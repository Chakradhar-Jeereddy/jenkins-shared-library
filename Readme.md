# 🚀 Jenkins Shared Library – Centralized CI/CD Pipeline

# ❗ Problem Statement

In many organizations:

* 👨‍💻 **Jenkinsfile** is owned and maintained by the **DevOps team**.
* 💻 **Application source code repository** (for example, `catalogue`) is owned and maintained by the **development team**.

This creates several challenges:

* ❌ It is not practical for the DevOps team to maintain Jenkinsfiles inside every developer repository.
* ❌ DevOps cannot easily update pipelines when the Jenkinsfile resides in developer-owned repositories.
* 🔒 It introduces security concerns and ownership conflicts.
* 🔄 Even small pipeline changes require updating multiple repositories if there is no reusable/shared pipeline.
* 📉 Maintaining consistent CI/CD standards across all projects becomes difficult.

---

# ✅ Solution: Jenkins Shared Library

A **Jenkins Shared Library** centralizes common pipeline logic and allows multiple projects to reuse the same pipeline.

## ⭐ Benefits

* 📚 Centralized Jenkins pipeline that can be reused by multiple projects.
* 🔁 Pipeline logic is maintained as reusable functions/libraries.
* 🛠️ Changes are made in one place and automatically apply to all consuming projects.
* 📏 Enforces consistent CI/CD standards across all repositories.
* ♻️ Improves maintainability and reduces duplication.
* ⚙️ Pipelines can be parameterized to support different applications.
* 📤 Developer repositories only pass the required parameters to the shared library.
* 🖥️ Dedicated Jenkins agents can be assigned based on:

  * Programming language
  * Build tool
  * Deployment platform

---

# 🧰 Example Technology Matrix

| 💻 Programming Language | 🔨 Build Tool             | 🚀 Deployment Platform      |
| ----------------------- | ------------------------- | --------------------------- |
| Java                    | Maven / Gradle            | Tomcat / JBoss / Kubernetes |
| Python                  | pip / Poetry / setuptools | VM / Docker / Kubernetes    |
| JavaScript (Node.js)    | npm / yarn / pnpm         | VM / Docker / Kubernetes    |
| Go                      | Go Modules                | VM / Docker / Kubernetes    |

---

# 📂 Shared Library Repository Structure

Create a dedicated repository:

```text
jenkins-shared-library/
└── vars/
    └── nodeJSEKSPipeline.groovy
```

Inside the Groovy file, expose a reusable pipeline function.

```groovy
call(Map configMap) {
    pipeline {
        ...
    }
}
```

## 📝 Notes

* 📥 The pipeline accepts a `Map` as input.
* 👨‍💻 The developer passes configuration values through this map.
* ⚙️ The shared library uses those values during pipeline execution.

---

# ⚙️ Configure the Shared Library in Jenkins

Navigate to:

```text
Jenkins
└── Manage Jenkins
    └── System
        └── Global Trusted Pipeline Libraries
```

Configure the shared library as follows:

| Setting            | Value                                                               |
| ------------------ | ------------------------------------------------------------------- |
| 📛 Name            | `jenkins-shared-library`                                            |
| 🌿 Default Version | `main`                                                              |
| ✅ Load Implicitly  | Enabled                                                             |
| 📦 SCM             | Git                                                                 |
| 🔗 Repository URL  | `https://github.com/Chakradhar-Jeereddy/jenkins-shared-library.git` |

## 🔐 About Global Trusted Pipeline Libraries

Shared libraries configured here:

* 🌍 Are available to all Pipeline jobs.
* 🔓 Run without Jenkins sandbox restrictions.
* 📦 Can use features such as `@Grab`.
* 🛡️ Allow centralized pipeline management.

---

# 📄 Jenkinsfile in the Developer Repository

The developer repository contains only a minimal Jenkinsfile.

## 🎯 Responsibilities

* 📚 Load the shared library.
* 📨 Pass application-specific parameters.
* ☎️ Invoke the appropriate shared pipeline.
* 🌿 Execute the CI pipeline only for feature branches.
* 🚫 Prevent CI execution from the `main` branch and instruct users to follow the Change Release (CR) process.

Example:

```groovy
@Library('jenkins-shared-library') _

// vars/nodeJSEKSPipeline

def mymap = [
    project: 'roboshop',
    component: 'catalogue',
    acc_id: '406682759639'
]

if (!env.BRANCH_NAME.equalsIgnoreCase('main')) {
    nodeJSEKSPipeline(mymap)
} else {
    echo "Please follow the CR process"
}
```

---

# 🔄 CI/CD Workflow

1. 🌿 Developer pushes code to a feature branch.
2. 🔍 Jenkins Multibranch Pipeline automatically discovers the branch.
3. 📚 Jenkins loads the shared library at runtime.
4. 🏗️ Shared pipeline performs the CI build.
5. 🐳 Docker image is built and pushed to the central image repository.
6. 🚀 Shared pipeline triggers the downstream deployment pipeline.
7. ⛵ Deployment pipeline deploys the application using Helm charts.
8. 📁 Deployment repository used:

```text
catalogue-deploy
```

---

# 🌳 Multibranch Pipeline

Create a **Multibranch Pipeline** using the developer repository.

### ✅ Benefits

* 🔍 Automatically discovers all branches.
* ➕ Automatically creates jobs for new branches.
* 🏗️ Builds feature branches without manual configuration.
* 🌱 Provides branch-specific CI execution.

---

# ⛵ Helm Deployment

The deployment pipeline uses Helm for Kubernetes deployments.

## 📦 `helm upgrade --install`

```bash
helm upgrade --install
```

### Behavior

* ➕ Installs the release if it does not exist.
* 🔄 Upgrades the release if it already exists.

---

## 🛡️ `--atomic`

```bash
--atomic
```

### Behavior

* ⏳ Waits for the deployment to complete.
* ↩️ Automatically rolls back if deployment fails.
* ⌛ Combined with:

```bash
--wait --timeout=5m
```

the deployment waits for **up to 5 minutes** before determining success or failure.

---

# 🚀 Deployment Stage Example

```groovy
stage('Deploy') {
    steps {
        script {
            withAWS(region: 'us-east-1', credentials: 'aws-creds') {
                sh """
                    aws eks update-kubeconfig --region ${REGION} --name ${PROJECT}-${deploy_to}

                    kubectl get nodes

                    echo ${deploy_to}
                    echo ${appVersion}

                    helm upgrade --install ${COMPONENT} \
                        -f values-${deploy_to}.yaml \
                        -n ${PROJECT} \
                        --atomic \
                        --wait \
                        --timeout=5m \
                        .
                """
            }
        }
    }
}
```

---

# 🏗️ Overall Architecture

```text
               👨‍💻 Developer Repository
                         │
                         │ Jenkinsfile
                         ▼
              📚 Load Shared Library
                         │
                         ▼
          ⚙️ nodeJSEKSPipeline(Map)
                         │
                         ▼
                  🏗️ Build & Test
                         │
                         ▼
               🐳 Build Docker Image
                         │
                         ▼
           📦 Push Image to Registry
                         │
                         ▼
      🚀 Trigger Deployment Pipeline
                         │
                         ▼
        📁 catalogue-deploy Repository
                         │
                         ▼
              ⛵ Helm Upgrade/Install
                         │
                         ▼
              ☸️ Amazon EKS Cluster
```
