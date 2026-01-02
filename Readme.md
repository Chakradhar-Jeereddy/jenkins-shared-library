Problem statement
==
- Owner of the jenkins file is DevOps engineer.
- Owner of the catalogue repo is Developer.
- It is not feasible for DevOps guy to manage Jenkins file in developers repo.
- Devops can't make changes to Jenkinfile/pipeline when it is in developers repo.
- Creates secruity risk and conflicts.
- For small changes, we have to change many pipelines, if no reusable/shareable pipeline.
- Not posible to maintain same standards.

Jenkins Shared Library(Keep pileline here)
==
- Centralized jenkins pipeline that can be resused at runtime.
- Pipeline as library or function.
- Easy to update when changes occur and enforce standards.
- Reusability of the pipeline.
- For all nodejs projects one pipeline of that kind can work as function.
- We parameterize the pipeline and keep it in shared library.
- Developer repo passes parameters to pipleline using the Jenkinsfile and it refer/call the pipeline stored in the library.
- We can have dedicated agents based on build type and language, can be reused.

| Programming Language | Build Tool              | Deployment Platform              |
|----------------------|-------------------------|----------------------------------|
| Java                 | Maven / Gradle          | Tomcat / JBoss / Kubernetes      |
| Python               | pip / Poetry / setuptools | VM / Docker / Kubernetes        |
| JavaScript (Node.js) | npm / yarn / pnpm       | VM / Docker / Kubernetes         |
| Go                   | Go modules              | VM / Docker / Kubernetes         |

Create a repo
```
jenkins-shared-library
  vars
    nodeJSEKSPipeline.groovy

## Add a function that can call the pipeline
## Pass a Map variable into the function.
## The parameters for the Map variable will be passed by developer.
call(Map configMap){
 pipleline{
 }
}
```

Create shared linrary under Global Trusted Pipeline Libraries
Sharable libraries available to any Pipeline jobs running on this system. 
These libraries will be trusted, meaning they run without “sandbox” restrictions and may use @Grab.
==
- Jenkins -> Manage -> System -> add
    * Name: jenkins-shared-library
    * Default version: Main
    * Load implicitly
    * SCM: git
    * URL: https://github.com/Chakradhar-Jeereddy/jenkins-shared-library.git

Create Jenkinsfile in developers repository
==
- Refer the shared library in fist line of jenkins file, the pipeline will be loaded at runtime.
- Use a function with the name of the variable passed in Call function in library.
- Pass the key, value parameters as requred.
- Put a condition to call the pipeline function only when it is feature branch as it is CI.
- It its a main branch inform to follow change release process.
- Create a multibranch CI pipline using the developer repository as source.
- The pipleline will automatically dicover all branches of the repo and clone all.
- When the pipline is tiggered from any feature branch, the CI will build and push the image to central repo
- After the CI build is completed, the jenkins shared pipeline triggers downstream pipeline.
- This will trigger another shared pipeline created for deployment and it deploys the application using helm charts.
- The deployment repo is catalgue-deploy
```
@Library('jenkins-shared-library') _ 

// /vars/nodeJSEKSPipeline
def mymap = [
     project: 'roboshop',
     component: 'catalogue',
     acc_id: '406682759639'
]

if( ! env.BRANCH_NAME.equalsIgnoreCase('main') ){
      nodeJSEKSPipeline(mymap)
}
else{
  echo "Please fallow the CR process"
}
```

helm command
==
- upgrade --install (It will install if no release exists, otherwise it will upgrade(apply changes)).
- --atomic : It will wait for 5 minutes and rollback in case the helm installation fails.
```
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
```



