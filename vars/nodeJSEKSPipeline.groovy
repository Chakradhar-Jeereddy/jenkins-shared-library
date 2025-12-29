// call is the default function like main
// Everything inside def call(){} is a function and we need to pass parameters
// def call(map configMap), we pass map key, value pairs
// We need to call this function from catalogue's Jenkinsfile
// Create Jenkinsfile in catalogue repo and refer this function as "library 'my-shared-library@master'"
def call(map configMap) {
	pipeline{
	 agent{
	  node{
	  label "agent"
	  }
	 }
	 parameters {
			string(name: 'PERSON', defaultValue: 'Mr Chakradhar', description: 'Who should I say hello to?')
			booleanParam(name: 'Deploy', defaultValue: false, description: 'Toggle this value')
	 }
	 environment{
	 course = "jenkins"
	 apiVersion = ""
	 acc_id = "406682759639"	
	 project = "chakra"
	 component = "catalogue"
	 }
	 options{
	  disableConcurrentBuilds()
	 }
	 stages{
		stage('Read json file'){
		steps{
		 script{
		 def packagejson = readJSON file: "package.json"
		 apiVersion = packagejson.version
		 echo "apiversion: ${apiVersion}"
		 }
		}
	  }
	  stage('Sonar Code Analysis') {
       when{
        expression { false }
       }
	   environment{
	   def scannerHome = tool 'sonar-8.0'
	   }
	   steps {
					script {
						withSonarQubeEnv('sonar-server') {
						sh "${scannerHome}/bin/sonar-scanner"
						}
					}
	   }
	  }
	  stage('Quality Gate') {
	   steps {
		  // Wait for Quality Gate, fail build if needed
		  timeout(time: 1, unit: 'HOURS') {
			waitForQualityGate abortPipeline: true
		  }
	   }
	  }      
	  stage('Build catalogue image'){
		steps{
		  withAWS(region:'us-east-1',credentials:'aws-ecr') {
		  sh"""
		  aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${acc_id}.dkr.ecr.us-east-1.amazonaws.com
		  docker build -t ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${apiVersion} .
		  docker push ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${apiVersion}
		  """
		  }
		}
	  }
	  stage('Test'){
		environment{
		name = "chakras testing"
		}
		steps{
		echo "Testing"
		echo "${name}"
		}
	  }
	  stage('Deploy'){
		when{
		expression { params.Deploy == "true" }
		}
		steps{
		echo "Deploying"
		}
	  }
	 }
	 post{
	  always{
	   cleanWs()
	   echo "Always say hi"
	  }
	  success{
	  echo "Passed the deployment"
	  }
	  failure{
	  echo "Deployment failed"
	  }
	  aborted{
	  echo "deployment aborted"
	  }
	 }
	} 
}