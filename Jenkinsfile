@Library('copado_coverage.groovy')
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
        groovy copado_coverage.groovy -f ${COPADO_COVERAGE_FEATURE_BRANCH} -i ${COPADO_COVERAGE_CLIENT_ID} -s ${COPADO_COVERAGE_CLIENT_SECRET} -u ${COPADO_COVERAGE_USER_NAME} -p ${COPADO_COVERAGE_PASSWORD}
      }
    }
  }
}