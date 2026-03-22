pipeline {
    agent any

    environment {
        // Define global environment variables for the pipeline
        DOCKER_REGISTRY = 'your-registry.dkr.ecr.us-east-1.amazonaws.com'
        BACKEND_IMAGE = 'adaptivebp-backend'
        FRONTEND_IMAGE = 'adaptivebp-frontend'
    }

    // Set some options for the pipeline run
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
    }

    stages {
        stage('Checkout') {
            steps {
                // Checkout the SCM (Git) repository
                checkout scm
            }
        }

        stage('Backend: Build & SAST') {
            steps {
                dir('Backend/api') {
                    echo 'Building Spring Boot Backend...'
                    sh 'chmod +x mvnw'
                    sh './mvnw clean package'
                    
                    echo 'Running SAST Security Scan via Docker...'
                    // Using Docker-based Semgrep for cleaner builds
                    sh "docker run --rm -v ${WORKSPACE}/Backend/api:/src returntocorp/semgrep semgrep ci --config=p/default || true"
                }
            }
        }

        stage('Frontend: Build & SAST') {
            steps {
                dir('Frontend') {
                    echo 'Building Angular Frontend...'
                    sh 'npm ci'
                    sh 'npm run build --configuration=production'
                    
                    echo 'Running SAST Security Scan via Docker...'
                    sh "docker run --rm -v ${WORKSPACE}/Frontend:/src returntocorp/semgrep semgrep ci --config=p/default || true"
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    echo 'Building Backend Container Image...'
                    dir('Backend/api') {
                        sh "docker build -t ${BACKEND_IMAGE}:${env.GIT_COMMIT} ."
                    }

                    echo 'Building Frontend Container Image...'
                    dir('Frontend') {
                        sh "docker build -t ${FRONTEND_IMAGE}:${env.GIT_COMMIT} ."
                    }
                }
            }
        }

        stage('Container Scanning (Trivy)') {
            steps {
                script {
                    echo 'Scanning Backend Container for OS/Library Vulnerabilities via Docker...'
                    // We mount the docker socket so the Trivy container can scan local host images
                    sh "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v ${WORKSPACE}/.trivy-cache:/root/.cache ghcr.io/aquasecurity/trivy:latest image --severity CRITICAL,HIGH --exit-code 1 --no-progress ${BACKEND_IMAGE}:${env.GIT_COMMIT}"
                    
                    echo 'Scanning Frontend Container...'
                    sh "docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v ${WORKSPACE}/.trivy-cache:/root/.cache ghcr.io/aquasecurity/trivy:latest image --severity CRITICAL,HIGH --exit-code 1 --no-progress ${FRONTEND_IMAGE}:${env.GIT_COMMIT}"
                }
            }
        }

        stage('Push & Deploy') {
            // Only deploy when changes are merged into specific branches
            when {
                anyOf {
                    branch 'develop'
                    branch 'main'
                }
            }
            steps {
                script {
                    echo "Pushing images to Container Registry..."
                    /* Example of pushing using Jenkins Credentials Binding plugin:
                    withDockerRegistry(credentialsId: 'aws-ecr-creds', url: "https://${DOCKER_REGISTRY}") {
                        sh "docker tag ${BACKEND_IMAGE}:${env.GIT_COMMIT} ${DOCKER_REGISTRY}/${BACKEND_IMAGE}:${env.GIT_COMMIT}"
                        sh "docker tag ${FRONTEND_IMAGE}:${env.GIT_COMMIT} ${DOCKER_REGISTRY}/${FRONTEND_IMAGE}:${env.GIT_COMMIT}"
                        sh "docker push ${DOCKER_REGISTRY}/${BACKEND_IMAGE}:${env.GIT_COMMIT}"
                        sh "docker push ${DOCKER_REGISTRY}/${FRONTEND_IMAGE}:${env.GIT_COMMIT}"
                    }
                    */
                    echo "Deployment triggered successfully!"
                }
            }
        }
    }

    post {
        always {
            // Clean up the workspace after the build to save disk space on the agent
            cleanWs()
        }
        success {
            echo "Pipeline completed successfully! ✅"
        }
        failure {
            echo "Pipeline failed! ❌ Please check the logs."
            // Example: slackSend(channel: '#ci-cd-alerts', message: "Build Failed: ${env.JOB_NAME} [${env.BUILD_NUMBER}]")
        }
    }
}
