// =======================================================================================================================================
// Class definition
// =======================================================================================================================================
class ClassCoverage {
    String packageName
    String fileName
    Double coverageRate


    String toString() {
        return sprintf("{\n\t'packageName':'%s',\n\t'fileName':'%s',\n\t'coverageRate':%f\n}", [packageName, fileName, coverageRate])
    }
}

class SalesforceService {
    private jsonSlurper = new groovy.json.JsonSlurper()

    String sessionToken
    String instanceUrl

    void getToken(String clientId, String clientSecret, String username, String password) {
        println "\n-------- SALESFORCE: Token request -------"

        // Do request
        String request_params = "grant_type=password&client_id=${clientId}&client_secret=${clientSecret}&username=${username}&password=${password}"
        def request = SalesforceService.buildPostRequest("https://login.salesforce.com/services/oauth2/token?${request_params}")
        println "\nResponse:\n" + request.getResponseMessage()
        assert request.getResponseCode() == 200

        // Parse request
        String salesforceTokenResponseJson = request.inputStream.text
        def salesforceTokenResponse = jsonSlurper.parseText(salesforceTokenResponseJson)
        sessionToken = "${salesforceTokenResponse.token_type} ${salesforceTokenResponse.access_token}"
        instanceUrl = salesforceTokenResponse.instance_url

        println "Instance URL: ${instanceUrl}"
    }

    String getUserStoryId(String featureBranch) {

        println "\n-------- SALESFORCE: Select user story (${featureBranch}) -------"
        String userStoryName = (featureBranch - ~/^feature\//).take(10)
        String query = "SELECT Id FROM User_Story__c WHERE Name='${userStoryName}'"
        query = query.replace(" ", "+")
        def request = new URL((String) ("${instanceUrl}/services/data/v20.0/query?q=${query}")).openConnection() as HttpURLConnection
        request.setRequestProperty('Authorization', sessionToken)
        request.setRequestProperty('Accept', 'application/json')
        request.setRequestProperty('Content-Type', 'application/json')
        request.requestMethod = "GET"

        println "\nResponse:\n" + request.getResponseMessage()
        assert request.getResponseCode() == 200

        String requestJson = request.inputStream.text
        println requestJson

        def userStory = jsonSlurper.parseText(requestJson)

        return userStory.records[0].Id
    }

    void saveUserStory(String userStoryId, Double minRate) {

        println "\n-------- SALESFORCE: Set coverage rate on user story -------"
        def userStoryEditParams = [:]
        userStoryEditParams['Has_Apex_Code__c'] = true
        userStoryEditParams['Apex_Code_Coverage__c'] = minRate
        String userStoryEditParamsStr = groovy.json.JsonOutput.toJson(userStoryEditParams)

        def userStoryEditRequest = SalesforceService.buildPatchRequest("${instanceUrl}/services/data/v20.0/sobjects/User_Story__c/${userStoryId}", sessionToken, userStoryEditParamsStr)



        println "\nResponse:\n" + userStoryEditRequest.getResponseMessage()
        assert userStoryEditRequest.getResponseCode() == 204

    }

    void uploadAttachment(String userStoryId) {

        println "\n-------- SALESFORCE: Upload attachment -------"
        String date = new Date().format('yyyyMMdd_HHmmss_SSS')
        println date
        def attachment = [:]
        attachment['name'] = "coverage${date}.zip"
        attachment['Body'] = new String(java.util.Base64.getEncoder().encode(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("target/site/cobertura.zip"))))
        attachment['parentId'] = userStoryId

        String attachmentStr = groovy.json.JsonOutput.toJson(attachment)
        def attachmentRequest = buildPostRequest("${instanceUrl}/services/data/v20.0/sobjects/attachment/", sessionToken, attachmentStr)

        println "\nResponse:\n" + attachmentRequest.getResponseMessage()
        assert attachmentRequest.getResponseCode() == 201
    }

    static HttpURLConnection buildPostRequest(String urlStr) {
        buildPostRequest(urlStr, null, null)
    }

    static HttpURLConnection buildPostRequest(String urlStr, String sessionToken, String body) {
        def request = new URL(urlStr).openConnection() as HttpURLConnection
        request.setRequestProperty('Accept', 'application/json')
        request.setRequestProperty("grant_type", "password")
        request.setRequestProperty('Content-Type', 'application/json')

        request.requestMethod = "POST"

        if (sessionToken != null) {
            request.setRequestProperty('Authorization', sessionToken)
        }

        if (body != null) {
            request.setDoOutput(true)
            def attachmentRequestBodyWriter = new BufferedWriter(new OutputStreamWriter(request.getOutputStream()))
            attachmentRequestBodyWriter.write(body)
            attachmentRequestBodyWriter.close()
        }

        return request
    }

    static HttpURLConnection buildPatchRequest(String urlStr, String sessionToken, String bodyContentStr) {
        println "Fields to be updated:\n" + bodyContentStr

        urlStr = urlStr + "?_HttpMethod=PATCH"
        def request = new URL(urlStr).openConnection() as HttpURLConnection
        request.setRequestProperty('Accept', 'application/json')
        request.setRequestProperty('Content-Type', 'application/json')
        request.setRequestProperty('Authorization', sessionToken)
        //  request.setRequestProperty("X-HTTP-Method-Override", "PATCH")
        request.setRequestProperty("grant_type", "password")
        request.requestMethod = "POST"
        request.setDoOutput(true)

        def httpRequestBodyWriter = new BufferedWriter(new OutputStreamWriter(request.getOutputStream()))
        httpRequestBodyWriter.write(bodyContentStr)
        httpRequestBodyWriter.close()

        return request
    }

}

class CoverageService {

    static String readCoverageFile() {
        return new File('./target/site/cobertura/coverage.xml').text
    }

    /**
     *
     * @return XML Parser which does not validate the content
     */
    static XmlSlurper buildXmlParser() {
        def parser = new XmlSlurper()
        parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        return parser
    }

    /**
     * Given a coverage.xml content as string, it will parse the content and returns a map with the Class/Coverage rates
     * @param coverageXml
     * @return
     */
    Map<String, ClassCoverage> retrieveCoverageClasses(XmlSlurper parser, String coverageXml) {
        def coverage = parser.parseText(coverageXml)
        def coverageClasses = [:]
        coverage.packages.'package'.each { packageNode ->
            packageNode.classes.class.each { clazz ->
                def cov = new ClassCoverage()
                cov.packageName = packageNode.@name
                cov.fileName = clazz.@filename
                cov.coverageRate = java.lang.Double.valueOf(clazz["@line-rate"].text())

                coverageClasses.put(cov.fileName, cov)
            }
        }

        println "-------- COBERTURA: Retrieved classes (${coverageClasses.values().size()}) -------"
        println coverageClasses.values()

        return coverageClasses
    }

    Double minRateCoverage(String changedFiles, Map<String, ClassCoverage> coverageClasses) {
        Double minRate = 999.0

        changedFiles.eachLine {
            it = it.trim()
            if (it.startsWith("src/main/java/")) {
                def processedFileName = it - ~/^src\/main\/java\//
                ClassCoverage cov = coverageClasses[processedFileName.toString()]
                if (cov.coverageRate < minRate) {
                    minRate = cov.coverageRate
                }
            }

        }

        println "\n-------- COBERTURA: Min Rate -------\n" + minRate
        assert minRate != 999.0

        return minRate
    }
}

// =======================================================================================================================================
// Method definition
// =======================================================================================================================================
static boolean shouldFinish(CliBuilder cli, String option) {
    println "Option: '${option}'"
    if (option.isEmpty()) {
        cli.usage()
        return true
    }
    return false
}


def updateCopadoCoverage(args) {

    // ****************************** INPUT PARAMETERS ******************************
    def cli = new CliBuilder(usage: 'copado_coverage.groovy -[chflms] [date] [prefix]')
    cli.with {
        h longOpt: 'help', 'Show usage information'
        i longOpt: 'client-id', args: 1, argName: 'id', 'Salesforce application client id'
        s longOpt: 'client-secret', args: 1, argName: 'secret', 'Salesforce application client secret'
        u longOpt: 'user-name', args: 1, argName: 'user', 'Salesforce username'
        p longOpt: 'password', args: 1, argName: 'password', 'Salesforce username password'
        f longOpt: 'feature-branch', args: 1, argName: 'name', 'Git feature branch name'
    }

    def options = cli.parse(args)
    if (!options) {
        return
    }

    if (options.h) {
        cli.usage()
        return
    }

    if (!options.i || !options.s || !options.u || !options.p || !options.f) {
        println "ERROR: Wrong parameters"

        cli.usage()
        return
    }

    // ****************************** Local variables ******************************
    XmlSlurper parser = CoverageService.buildXmlParser()
    SalesforceService salesforceService = new SalesforceService()
    CoverageService coverageService = new CoverageService()

    // ****************************** GIT ******************************
    String changedFiles = "git diff head~ --name-only".execute().getText()
    println "-------- GIT: Last changed files -------\n" + changedFiles

    // ****************************** COVERAGE ******************************
    String coverageXml = CoverageService.readCoverageFile()
    def coverageClasses = coverageService.retrieveCoverageClasses(parser, coverageXml)
    Double minRate = coverageService.minRateCoverage(changedFiles, coverageClasses)

    // ****************************** SALESFORCE ******************************
    salesforceService.getToken(options.i, options.s, options.u, options.p)
    def userStoryId = salesforceService.getUserStoryId(options.f)
    salesforceService.saveUserStory(userStoryId, minRate)
    salesforceService.uploadAttachment(userStoryId)
}

// =======================================================================================================================================
// Main
// =======================================================================================================================================
updateCopadoCoverage(args)