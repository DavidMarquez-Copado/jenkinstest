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
        sh '/usr/local/bin/git clone https://github.com/DavidMarquez-Copado/copado-coverage.git'
        sh 'java -jar copado-coverage/realses/copado-coverage.jar -clientId "${COPADO_COVERAGE_CLIENT_ID}" -clientSecret "${COPADO_COVERAGE_CLIENT_SECRET}" -username "${COPADO_COVERAGE_USER_NAME}" -password "${COPADO_COVERAGE_PASSWORD}" -featureBranch "${COPADO_COVERAGE_FEATURE_BRANCH}"' 
      }
    }
  }
  post {
    always {
        step([$class: 'CoberturaPublisher', autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: '**/coverage.xml', failUnhealthy: false, failUnstable: false, maxNumberOfBuilds: 0, onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false])
    }
  }
}