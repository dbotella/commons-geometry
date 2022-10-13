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
		stage('Coverity Pull request Scan') {
			steps {
				withCoverityEnvironment(coverityInstanceUrl: "$CONNECT", projectName: "$PROJECT", streamName: "$PROJECT") {
					sh '''
                        rm -rf /tmp/idir_pull
						# cov-run-desktop --dir /tmp/idir_pull --url $COV_URL --stream $COV_STREAM --build mvn -B clean package -DskipTests
						# sed -i 's#/var/lib/jenkins/workspace/commons-geometry/#$(pwd)#' /tmp/idir/export.json > import.json
						cat  /tmp/idir/export.json > import.json
						cov-manage-emit --dir /tmp/idir_pull import-json-build --input-file import.json
						cov-run-desktop --dir /tmp/idir_pull --url $COV_URL --stream $COV_STREAM --present-in-reference false \
							--ignore-uncapturable-inputs true --text-output issues.txt $CHANGE_SET
						if [ -s issues.txt ]; then cat issues.txt; touch issues_found; fi
					'''
				}
				script { // Coverity Quality Gate
					if (fileExists('issues_found')) { unstable 'issues detected' }
				}
			}
		}
	}

	post {
		always {
			cleanWs()
		}
	}
}

