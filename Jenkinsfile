pipeline {
  agent any
  stages {
    stage('list-env') {
      steps {
        bat 'set'
      }
    }
    stage('build') {
        steps {
            bat 'mvn clean install'
        }
    }
    stage('coverage') {
      steps {
        bat 'mvn -f pom.xml clean org.codehaus.mojo:cobertura-maven-plugin:2.7:cobertura -Dcobertura.report.format=xml'
        bat 'mvn -f pom.xml org.codehaus.mojo:cobertura-maven-plugin:2.7:cobertura -Dcobertura.report.format=html'
        bat '7z a -tzip target/site/cobertura.zip target/site/cobertura'
        bat 'git clone https://${GIT_USERNAME_PASSWORD}@github.com/CopadoSolutions/copado-coverage.git'
        bat 'java -jar copado-coverage/realses/copado-coverage.jar -clientId ${COPADO_COVERAGE_CLIENT_ID} -clientSecret ${COPADO_COVERAGE_CLIENT_SECRET} -username ${COPADO_COVERAGE_USER_NAME} -password ${COPADO_COVERAGE_PASSWORD} -featureBranch ${COPADO_COVERAGE_FEATURE_BRANCH}'
      }
    }
  }
  post {
    always {
        step([$class: 'CoberturaPublisher', autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: '**/coverage.xml', failUnhealthy: false, failUnstable: false, maxNumberOfBuilds: 0, onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false])
    }
  }
}