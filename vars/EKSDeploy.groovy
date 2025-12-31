def call(Map mymap){ 
 pipeline {
    // These are pre-build sections
    agent {
        node {
            label 'agent'
        }
    }
    environment {
        COURSE = "Jenkins"
        appVersion = mymap.get("appVersion")
        ACC_ID = mymap.get("acc_id")
        PROJECT = mymap.get("project")
        COMPONENT = mymap.get("component")
        REGION = mymap.get("region")
        deploy_to = mymap.get("deploy_to")
    }
    options {
        timeout(time: 30, unit: 'MINUTES') 
        disableConcurrentBuilds()
    }
    parameters {
        string(name: 'appVersion', description: 'Which app version you want to deploy')
        choice(name: 'deploy_to', choices: ['dev', 'qa', 'prod'], description: 'Pick something')
    }
    // This is build section
    stages {
        
        stage('Deploy') {
            steps {
                script{
                    withAWS(region:'us-east-1',credentials:'aws-creds') {
                        sh """
                            aws eks update-kubeconfig --region ${REGION} --name ${PROJECT}-${deploy_to}
                            kubectl get nodes
                            echo ${deploy_to}, echo ${appVersion}
                            helm upgrade --install ${COMPONENT} -f values-${deploy_to}.yaml -n ${PROJECT} --atomic --wait --timeout=5m .
                        """
                    }
                }
            }
        }
        
    }
    post{
        always{
            echo 'I will always say Hello again!'
            cleanWs()
        }
        success {
            echo 'I will run if success'
        }
        failure {
            echo 'I will run if failure'
        }
        aborted {
            echo 'pipeline is aborted'
        }
    }
 }
}