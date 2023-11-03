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
    stage('Yarn build') {
      parallel {
        stage('yarn build') {
          steps {
            container('yarn') {
              sh """
                yarn --registry=https://registry.npm.taobao.org && yarn build
                rm -rf /usr/local/share/.cache/yarn/v6/${PROJECT_NAME}/*
                cp -r dist /usr/local/share/.cache/yarn/v6/${PROJECT_NAME}/
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
