def PROJECT_NAME = "${env.gitlabSourceRepoName}"
def HARBOR_URL= "registry.zet-fl.com/bsd"
def NEW_IMAGE_TAG = "${env.gitlabSourceBranch.split("/")[1]}"
def TARGET_CLUSTER = "${credentials('kubeconfig-172.20.8.55')}"

pipeline {
  options {
    quietPeriod(0)
  }

  agent {
      kubernetes {
          yamlFile 'kubernetes-yaml/kubernetes-pod.yaml'
      }
  }

  stages {
    stage('Pull code') {
      steps {
        git branch: "${env.gitlabSourceBranch}", credentialsId: 'git-cert', url: "${env.gitlabSourceRepoHttpUrl}"
      }
    }
    stage('maven build') {
      parallel {
        stage('Maven clean') {
          steps {
            container('maven') {
              sh """
                mvn clean package -Dmaven.test.skip=true
              """
            }
          }
        }
      }
    }
    stage('Build image') {
      steps {
        container('docker') {
          sh """
            docker build -t ${HARBOR_URL}/${PROJECT_NAME}:${NEW_IMAGE_TAG} .
          """
        }
      }
    }
    stage('Push image to Harbor') {
      steps {
        container('docker') {
          withCredentials([usernamePassword(credentialsId: 'harbor-cert', usernameVariable: 'REGISTRY_USER', passwordVariable: 'REGISTRY_PASSWORD')]) {
              sh 'docker login registry.zet-fl.com -u $REGISTRY_USER -p $REGISTRY_PASSWORD'
          }
          sh "docker push ${HARBOR_URL}/${PROJECT_NAME}:${NEW_IMAGE_TAG}"
        }
      }
    }
    stage('Delivery') {
      steps {
        container('kubectl') {
          withKubeConfig([credentialsId: 'kubeconfig-172.20.8.55']){
            script {
              def namespaces = ['bsd-1000', 'bsd-1100', 'bsd-2000']
              for (def namespace in namespaces) {
                sh "kubectl -n${namespace} set image deployment/${PROJECT_NAME} ${PROJECT_NAME}=${HARBOR_URL}/${PROJECT_NAME}:${NEW_IMAGE_TAG}"
                sh "kubectl get pod -n${namespace} -l app=${PROJECT_NAME} -oname |xargs kubectl -n${namespace} delete --force=true"
              }
            }
          }
        }
      }
    }
  }
}
