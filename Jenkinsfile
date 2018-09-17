pipeline {
  agent any
  stages {
    stage('list-env') {
      steps {
        sh 'env'
      }
    }
    stage('build') {
        steps {
            sh '/usr/local/bin/mvn clean install'
        }
    }
    stage('coverage') {
      steps {
        sh '/usr/local/bin/mvn -f pom.xml clean org.codehaus.mojo:cobertura-maven-plugin:2.7:cobertura -Dcobertura.report.format=xml'
        sh '/usr/local/bin/mvn -f pom.xml org.codehaus.mojo:cobertura-maven-plugin:2.7:cobertura -Dcobertura.report.format=html'
        sh 'zip -r target/site/cobertura.zip target/site/cobertura'

        library('copadoCoverage@')
        sh 'updateCopadoCoverage "-f" ${COPADO_COVERAGE_FEATURE_BRANCH} "-i" ${COPADO_COVERAGE_CLIENT_ID} "-s" ${COPADO_COVERAGE_CLIENT_SECRET} "-u" ${COPADO_COVERAGE_USER_NAME} "-p" ${COPADO_COVERAGE_PASSWORD}'
      }
    }

  }
  post {
    always {
        step([$class: 'CoberturaPublisher', autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: '**/coverage.xml', failUnhealthy: false, failUnstable: false, maxNumberOfBuilds: 0, onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false])
    }
  }
}