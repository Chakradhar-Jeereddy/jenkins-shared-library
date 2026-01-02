// call function to use pipleline as function
def call (Map mymap){
 pipeline{
    // These are prebuild sections
    agent{
        node{
            label "agent"
        }
    }
    environment{
        appVersion=""
        ACC_ID=mymap.get('acc_id')
        PROJECT=mymap.get('project')
        COMPONENT=mymap.get('component')
    }
    options{
        timeout(time: 10, unit: 'MINUTES')
        disableConcurrentBuilds()
    }
    // These are build sections
    stages{
        stage('Read Version'){
            steps{
                script{
                def packageJson = readJSON file: 'package.json'
                appVersion = packageJson.version
                echo "app version: ${appVersion}"
                }
            }
        }
        stage('Install Dependencies') {
                steps {
                    script{
                        sh """
                            npm install
                        """
                    }
                }
        }
        stage('Unit Test') {
                steps {
                    script{
                        sh """
                            echo test
                        """
                    }
                }
        }
        stage('Build image'){
            steps{
             script{
              withAWS(region:'us-east-1', credentials:'aws-creds'){
                sh"""
                aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com
                docker build -t ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                docker push ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                """
              }
             }
            }
        }
        stage('Trigger Dev Deploy'){
            steps{
                script{
                    build job: "../${COMPONENT}-deploy",
                    wait: false,  //Wait for completion
                    propagate: false //Propogate status
                      parameters: [
                          string(name: 'appVersion', value: "${appVersion}"),
                          // You can add more parameters here
                          string(name: 'deploy_to', value: "dev")
                      ]
         
                }
            }
        }
    }
    post{
      always{
       cleanWs()
       echo "I always say hello again"
      }
      success{
       echo "I will run if success"
      }
      failure{
       echo "I will run if failure"
      }
      aborted{
       echo "Pipeline is aborted"
      }
    }
 }
}