#!/usr/bin/env groovy
/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

// https://github.com/synopsys-sig-community/synopsys-action-demo/blob/main/Jenkinsfile 

// Synopsys Coverity for Jenkins
// https://synopsys.atlassian.net/wiki/spaces/INTDOCS/pages/623018/Synopsys+Coverity+for+Jenkins
// https://synopsys.atlassian.net/wiki/spaces/INTDOCS/pages/992576193/Pipeline+job+using+CoverityIssueCheck
// https://synopsys.atlassian.net/wiki/spaces/INTDOCS/pages/1148420116/Pipeline+job+using+withCoverityEnvironment

pipeline {
	agent any

	environment {
		CONNECT = 'http://coverity.local.synopsys.com:8080'
		PROJECT = 'commons-geometry'
	}

	tools {
		jdk 'java-11-amazon-corretto'
        maven '/opt/apache-maven-3.6.0' 
	}

	stages {
		stage('Build') {
			steps {
				sh 'mvn -B compile'
			}
		}
		stage('Test') {
			steps {
				sh 'mvn -B test'
			}
		}
		stage('Coverity Full Scan') {
			when {
				allOf {
					not { changeRequest() }
					expression { BRANCH_NAME ==~ /(master|stage|release)/ }
				}
			}
			steps {
				withCoverityEnvironment(coverityInstanceUrl: "$CONNECT", projectName: "$PROJECT", streamName: "$PROJECT-$BRANCH_NAME") {
					sh '''
						cov-build --dir idir --fs-capture-search $WORKSPACE mvn -B clean package -DskipTests
						cov-analyze --dir idir --ticker-mode none --strip-path $WORKSPACE --webapp-security
						cov-commit-defects --dir idir --ticker-mode none --url $COV_URL --stream $COV_STREAM \
							--description $BUILD_TAG --target Linux_x86_64 --version $GIT_COMMIT
					'''
					script { // Coverity Quality Gate
						count = coverityIssueCheck(viewName: 'OWASP Web Top 10', returnIssueCount: true)
						if (count != 0) { unstable 'issues detected' }
					}
				}
			}
		}
		stage('Coverity Incremental Scan') {
			when {
				allOf {
					changeRequest()
					expression { CHANGE_TARGET ==~ /(master|stage|release)/ }
				}
			}
			steps {
				withCoverityEnvironment(coverityInstanceUrl: "$CONNECT", projectName: "$PROJECT", streamName: "$PROJECT-$CHANGE_TARGET") {
					sh '''
						export CHANGE_SET=$(git --no-pager diff origin/$CHANGE_TARGET --name-only)
						[ -z "$CHANGE_SET" ] && exit 0
						cov-run-desktop --dir idir --url $COV_URL --stream $COV_STREAM --build mvn -B clean package -DskipTests
						cov-run-desktop --dir idir --url $COV_URL --stream $COV_STREAM --present-in-reference false \
							--ignore-uncapturable-inputs true --text-output issues.txt $CHANGE_SET
						if [ -s issues.txt ]; then cat issues.txt; touch issues_found; fi
					'''
				}
				script { // Coverity Quality Gate
					if (fileExists('issues_found')) { unstable 'issues detected' }
				}
			}
		}
		stage('Deploy') {
			when {
				expression { BRANCH_NAME ==~ /(master|stage|release)/ }
			}
			steps {
				sh 'mvn -B install'
			}
		}
	}
	post {
		always {
			cleanWs()
		}
	}
}