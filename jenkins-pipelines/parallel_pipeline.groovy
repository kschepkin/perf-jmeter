pipeline {
    agent any
    
    environment {
        server1="login@127.0.0.1"
        server2="OFF"
        server3="OFF"
        server4="OFF"
        jmeterversion="5.5"
        project="mytestprojectname"
        preparecommand="rm -rf ./perf && mkdir perf && mkdir perf/results && unzip perf.zip -d perf"
        startcommand="export _JAVA_OPTIONS='-Xms3072m -Xmx6144m' && cd perf && ./apache-jmeter/bin/jmeter.sh -n -t ~/perf/scripts/${project}/${PERF_TEST}.jmx -Jthreads=${threads} -Jrampup=${rampup} -Jduration=${duration} -Jloops=${loops} -l ~/perf/results/${PERF_TEST}/${BUILD_NUMBER}.jtl"
        killcommand="kill -9 $(ps aux | grep jmeter | grep -v \"grep\" | awk '{print $2}')"
    }

    stages {

      stage('Get Artifacts') {
            
            steps {
                sh "rm -rf ./*"
                git credentialsId: 'gitlab', url: 'https://gitlab.com/konstantin.schepkin/perf.git'
                sh "wget https://apache-mirror.rbc.ru/pub/apache/jmeter/binaries/apache-jmeter-${jmeterversion}.zip -O ./apache-jmeter.zip"
                sh "unzip ./apache-jmeter.zip -d ./ && mv apache-jmeter-${jmeterversion} apache-jmeter"
                sh "cp -R ./plugins/* ./apache-jmeter/lib/ext/"
                sh "chmod 0755 ./apache-jmeter/bin/jmeter.sh"
                sh "zip -r perf.zip apache-jmeter scripts/${project}"
            }
        }

      stage('Copy artifacts') {

            steps {
            parallel(
                        "Load1": {
                            script {
                                if (server1 != "OFF") {
                                    sh "scp perf.zip ${server1}:~"
                                }
                            }
                        },
                        "Load2": {
                            script {
                                if (server2 != "OFF") {
                                    sh "scp perf.zip ${server2}:~"
                                } 
                            }
                        },
                        "Load3": {
                            script {
                                if (server3 != "OFF") {
                                    sh "scp perf.zip ${server3}:~"
                                }
                            }
                        },
                        "Load4": {
                            script {
                                if (server4 != "OFF") {
                                    sh "scp perf.zip ${server4}:~"
                                }
                            }
                        }
            )
        }
    }



stage('Prepare loaders') {
  steps {
            parallel(
                        "Load1": {
                            script {
                                if (server1 != "OFF") {
                                    sh "ssh  ${server1} \"${preparecommand}\""
                                }
                            }
                        },
                        "Load2": {
                            script {
                                if (server2 != "OFF") {
                                    sh "ssh  ${server2} \"${preparecommand}\""
                                }
                            }
                        },
                        "Load3": {
                            script {
                                if (server3 != "OFF") {
                                    sh "ssh  ${server3} \"${preparecommand}\""
                                }
                            }
                        },
                        "Load4": {
                            script {
                                if (server4 != "OFF") {
                                    sh "ssh  ${server4} \"${preparecommand}\""
                                }
                            }
                        }
              )        
    }
}
      stage('Run test') {

            steps {
            parallel(
                        "Load1": {
                            script {
                                if (server1 != "OFF") {
                                    sh "ssh  ${server1} \"${startcommand}\""
                                }
                            }
                        },
                        "Load2": {
                            script {
                                if (server2 != "OFF") {
                                    sh "ssh  ${server2} \"${startcommand}\""
                                }
                            }
                        },
                        "Load3": {
                            script {
                                if (server3 != "OFF") {
                                    sh "ssh  ${server3} \"${startcommand}\""
                                }
                            }
                        },
                        "Load4": {
                            script {
                                if (server4 != "OFF") {
                                    sh "ssh  ${server4} \"${startcommand}\""
                                }
                            }
                        }
              ) 
        }
    
      post {
        always {
                            script {
                                if (server1 != "OFF") {
                                    sh "ssh  ${server1} \"${killcommand}\""
                                }
                            
                                if (server2 != "OFF") {
                                    sh "ssh  ${server2} \"${killcommand}\""
                                }
                        
                                if (server3 != "OFF") {
                                    sh "ssh  ${server3} \"${killcommand}\""
                                }
                        
                                if (server4 != "OFF") {
                                    sh "ssh  ${server4} \"${killcommand}\""
                                }
                            }
                        } 
        }
    }
  } 
}
