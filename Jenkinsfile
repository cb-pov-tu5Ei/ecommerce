pipeline {
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
metadata:
  labels:
    jenkins: agent
    job: ecommerce-build
spec:
  containers:
  - name: maven
    image: maven:3.9.12-eclipse-temurin-17
    command:
    - cat
    tty: true
    volumeMounts:
    - name: maven-repo
      mountPath: /root/.m2/repository
  volumes:
  - name: maven-repo
    emptyDir: {}
"""
        }
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=/root/.m2/repository'
        ARTIFACT_VERSION = '0.0.1-SNAPSHOT'
        ARTIFACTORY_URL = 'https://artifactory.tjx-poc.cb-demos.io'
    }

    stages {
        stage('Checkout') {
            steps {
                container('maven') {
                    checkout scm
                    script {
                        sh '''
                            echo "=== Git Information ==="
                            echo "Commit: $(git rev-parse HEAD)"
                            echo "Branch: $(git rev-parse --abbrev-ref HEAD)"
                            echo "Author: $(git log -1 --pretty=format:'%an <%ae>')"
                            echo "Message: $(git log -1 --pretty=format:'%s')"
                            chmod +x mvnw
                        '''
                    }
                }
            }
        }

        stage('Build') {
            steps {
                container('maven') {
                    sh '''
                        echo "=== Building Spring Boot Application ==="
                        ./mvnw clean compile
                    '''
                }
            }
        }

        stage('Test') {
            steps {
                container('maven') {
                    sh '''
                        echo "=== Running Unit and Integration Tests ==="
                        echo "Selenium tests excluded by surefire configuration"
                        ./mvnw test -Dtest.delays.enabled=false
                    '''
                }
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml',
                          allowEmptyResults: false,
                          skipPublishingChecks: false
                }
            }
        }

        stage('Package') {
            steps {
                container('maven') {
                    sh '''
                        echo "=== Packaging Application ==="
                        ./mvnw package -DskipTests
                        echo "=== Build Artifact Info ==="
                        ls -lh target/*.jar
                    '''
                }
            }
        }

        stage('Deploy to Artifactory') {
            when {
                branch 'main'
            }
            steps {
                container('maven') {
                    withCredentials([usernamePassword(
                        credentialsId: 'artifactory-credentials',
                        usernameVariable: 'ARTIFACTORY_USERNAME',
                        passwordVariable: 'ARTIFACTORY_PASSWORD'
                    )]) {
                        sh '''
                            echo "=== Configuring Maven Settings ==="
                            mkdir -p ~/.m2
                            cat > ~/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>artifactory-releases</id>
            <username>${env.ARTIFACTORY_USERNAME}</username>
            <password>${env.ARTIFACTORY_PASSWORD}</password>
        </server>
        <server>
            <id>artifactory-snapshots</id>
            <username>${env.ARTIFACTORY_USERNAME}</username>
            <password>${env.ARTIFACTORY_PASSWORD}</password>
        </server>
    </servers>
</settings>
EOF
                            echo "=== Deploying to Artifactory ==="
                            echo "Target: ${ARTIFACTORY_URL}/artifactory/libs-snapshot"
                            ./mvnw deploy -DskipTests 2>&1 | tee deploy.log
                            echo "=== Deployment Complete ==="

                            # Extract the actual JAR URL from Maven output (only Artifactory uploads)
                            ARTIFACT_URL=$(grep "Uploading to artifactory-snapshots:" deploy.log | grep "\.jar$" | grep -o "https://[^ ]*\.jar" | head -1)
                            echo "Artifact URL: $ARTIFACT_URL"
                            echo "$ARTIFACT_URL" > artifact_url.txt
                        '''
                        script {
                            def artifactUrl = sh(script: 'cat artifact_url.txt', returnStdout: true).trim()
                            registerBuildArtifactMetadata(
                                name: "ecommerce",
                                url: artifactUrl,
                                version: "${env.ARTIFACT_VERSION}",
                                type: "Maven"
                            )
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'target/*.jar',
                           fingerprint: true,
                           allowEmptyArchive: false
            echo """
                ========================================
                BUILD SUCCESSFUL
                ========================================
                Artifact: ecommerce-${ARTIFACT_VERSION}.jar
                Branch: ${env.BRANCH_NAME}
                Build: ${env.BUILD_NUMBER}
                ========================================
            """
        }
        failure {
            echo """
                ========================================
                BUILD FAILED
                ========================================
                Check console output for error details
                Build: ${env.BUILD_NUMBER}
                ========================================
            """
        }
        unstable {
            echo "Build unstable - some tests may have failed"
        }
    }
}
