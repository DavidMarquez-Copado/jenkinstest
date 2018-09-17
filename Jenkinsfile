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
    stage('check-for-changes') {
      steps {
        sh 'files="$(/usr/local/bin/git diff head~ --name-only)"; echo $files'
      }
    }
  }
}