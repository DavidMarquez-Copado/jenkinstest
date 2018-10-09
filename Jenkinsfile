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
            sh 'mvn clean install'
        }
    }
    stage('coverage') {
      steps {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'copado-jenkins-github-basicauth', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
          sh 'mvn -f pom.xml clean org.codehaus.mojo:cobertura-maven-plugin:2.7:cobertura -Dcobertura.report.format=xml'
          sh 'mvn -f pom.xml org.codehaus.mojo:cobertura-maven-plugin:2.7:cobertura -Dcobertura.report.format=html'
          sh 'tar -zcvf target/site/cobertura.tar.gz target/site/cobertura'
          sh 'git clone https://$GIT_USERNAME:$GIT_PASSWORD@github.com/CopadoSolutions/copado-coverage.git'
          sh 'java -jar copado-coverage/releases/copado-coverage.jar -username "${COPADO_COVERAGE_USER_NAME}" -password "${COPADO_COVERAGE_PASSWORD}" -featureBranch "feature/US-0003757"'
          sh 'rm -rf copado-coverage'
        }
      }
    }
  }
  post {
    always {
        step([$class: 'CoberturaPublisher', autoUpdateHealth: false, autoUpdateStability: false, coberturaReportFile: '**/coverage.xml', failUnhealthy: false, failUnstable: false, maxNumberOfBuilds: 0, onlyStable: false, sourceEncoding: 'ASCII', zoomCoverageChart: false])
    }
  }
}