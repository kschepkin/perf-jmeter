pipeline {
    agent any
    
    environment {
        server1="user@127.0.0.1"
        server2="login@192.168.1.2"
        server3="OFF"
        server4="OFF"
 
    }

    stages {

      stage('Get Artifacts') {
            
            steps {
                sh "rm -rf ./*"
                git credentialsId: 'Credentinals-Name', url: 'https://repo.url/repo.git'
                sh "unzip ./apache-jmeter-5.3.zip -d ./"
                sh "chmod 0755 ./apache-jmeter-5.3/bin/jmeter.sh"
                sh "zip -r perf.zip apache-jmeter-5.3 scripts"
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
                                    sh "ssh  ${server1} \"rm -rf ./perf && mkdir perf && mkdir perf/results && unzip perf.zip -d perf \" "
                                }
                            }
                        },
                        "Load2": {
                            script {
                                if (server2 != "OFF") {
                                    sh "ssh  ${server2} \"rm -rf ./perf && mkdir perf && mkdir perf/results && unzip perf.zip -d perf \" "
                                }
                            }
                        },
                        "Load3": {
                            script {
                                if (server3 != "OFF") {
                                    sh "ssh  ${server3} \"rm -rf ./perf && mkdir perf && mkdir perf/results && unzip perf.zip -d perf \" "
                                }
                            }
                        },
                        "Load4": {
                            script {
                                if (server4 != "OFF") {
                                    sh "ssh  ${server4} \"rm -rf ./perf && mkdir perf && mkdir perf/results && unzip perf.zip -d perf \" "
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
                                    sh "ssh  ${server1} \"export _JAVA_OPTIONS='-Xms3072m -Xmx5120m' && cd perf && ./apache-jmeter-5.3/bin/jmeter.sh -n -t ~/perf/scripts/SL/${PERF_TEST}.jmx -l ~/perf/results/${PERF_TEST}/${BUILD_NUMBER}.jtl\" "
                                }
                            }
                        },
                        "Load2": {
                            script {
                                if (server2 != "OFF") {
                                    sh "ssh  ${server2} \"export _JAVA_OPTIONS='-Xms3072m -Xmx5120m' && cd perf && ./apache-jmeter-5.3/bin/jmeter.sh -n -t ~/perf/scripts/SL/${PERF_TEST}.jmx -l ~/perf/results/${PERF_TEST}/${BUILD_NUMBER}.jtl\" "
                                }
                            }
                        },
                        "Load3": {
                            script {
                                if (server3 != "OFF") {
                                    sh "ssh  ${server3} \"export _JAVA_OPTIONS='-Xms3072m -Xmx5120m' && cd perf && ./apache-jmeter-5.3/bin/jmeter.sh -n -t ~/perf/scripts/SL/${PERF_TEST}.jmx -l ~/perf/results/${PERF_TEST}/${BUILD_NUMBER}.jtl\" "
                                }
                            }
                        },
                        "Load4": {
                            script {
                                if (server4 != "OFF") {
                                    sh "ssh  ${server4} \"export _JAVA_OPTIONS='-Xms3072m -Xmx5120m' && cd perf && ./apache-jmeter-5.3/bin/jmeter.sh -n -t ~/perf/scripts/SL/${PERF_TEST}.jmx -l ~/perf/results/${PERF_TEST}/${BUILD_NUMBER}.jtl\" "
                                }
                            }
                        }
              ) 
        }
      }
  } 
}
