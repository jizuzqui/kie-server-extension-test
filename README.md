
Kie server extension that allows to operate with processes and tasks variables without using the class name in payloads by setting the kie-deployment-descriptor's environment entry BBVACustomUnmarshallingMode to new Boolean("true")


## curl examples (BBVACustomUnmarshallingMode = true)


**Start process instance**

    curl --location --request POST 'http://localhost:8080/kie-server/services/rest/business-process-management/v1/containers/ComplexDataProject_1.0.5-SNAPSHOT/process-models/ComplexDataProject.ComplexDataProcess/versions/1.0.5-SNAPSHOT:instance'  
    --header 'Content-Type: application/json'  
    --header 'Accept: application/json'  
    --data-raw '{ "mortgage" : { "amount" : 1234, "id" : 12345 } }'


**Get process variables**

    curl --location --request GET 'http://localhost:8080/kie-server/services/rest/business-process-management/v1/containers/ComplexDataProject_1.0.5-SNAPSHOT/instances/1745/variables'

**Response**: `{ "mortgage": { "id": 12345, "amount": 1234 }, "initiator": "jbpmAdmin" }`


**Update process variables**

    curl --location --request PUT 'http://localhost:8080/kie-server/services/rest/business-process-management/v1/containers/ComplexDataProject_1.0.5-SNAPSHOT/instances/1745/variables'  
    --header 'Content-Type: application/json'  
    --data-raw '{ "mortgage": { "id": 2222222, "amount": 333333 } }'

**Update task**

    curl --location --request PUT 'http://localhost:8080/kie-server/services/rest/business-process-management/v1/containers/ComplexDataProject_1.0.5-SNAPSHOT/tasks/954'  
    --header 'Content-Type: application/json'  
    --data-raw '{ "mortgageAmount" : 888 }'

**Complete task**

    curl --location --request POST 'http://localhost:8080/kie-server/services/rest/business-process-management/v1/containers/ComplexDataProject_1.0.5-SNAPSHOT/tasks/954:complete'  
    --header 'Content-Type: application/json'  
    --header 'Accept: application/json'  
    --header 'Authorization: Basic amJwbUFkbWluOnBhc3N3b3JkQDE='  
    --data-raw '{ "mortgage": { "id": 2222222, "amount": 333333 }}'

If BBVACustomUnmarshallingMode is false or null, request and response payloads related to mortgage variable would look like:

    { "mortgage": { "com.bpmn.complexdataproject.Mortgage" : { "id": 2222222, "amount": 333333 } }

