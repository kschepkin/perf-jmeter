pipeline {
    agent any
    
    environment {
        server1="user@127.0.0.1"
        server2="login@192.168.1.2"
        server3="OFF"
        server4="OFF"
    }
    
    stages {

        stage('Get Kill script') {
            
            steps {
                sh "rm -rf ./*"
                git credentialsId: 'Credentinals-Name', url: 'https://repo.url/repo.git'
            }
        }

      stage('Copy script') {

            steps {
            parallel(
                        "Load1": {
                            sh "scp scripts/kill.sh ${server1}:~"
                        },
                        "Load2": {
                            sh "scp scripts/kill.sh ${server2}:~"
                        },
                        "Load3": {
                            sh "scp scripts/kill.sh ${server3}:~"
                        },
                        "Load4": {
                            sh "scp scripts/kill.sh ${server4}:~"
                        }
            )
        }
    }

      stage('Kill') {
            

            steps {
                parallel(
                        "Load1": {
                            sh "ssh  ${server1} \"chmod +x kill.sh && bash kill.sh\""
                        },
                        "Load2": {
                            sh "ssh  ${server2} \"chmod +x kill.sh && bash kill.sh\""
                        },
                        "Load3": {
                            sh "ssh  ${server3} \"chmod +x kill.sh && bash kill.sh\""
                        },
                        "Load4": {
                            sh "ssh  ${server4} \"chmod +x kill.sh && bash kill.sh\""
                        }
              ) 
        }
      }
  } 
}