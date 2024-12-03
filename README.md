# Invoicing Service

## Description
The Service supports the invoicing in the billing process for DOME project.



## Swagger REST APIs
A Swagger APIs are available at this URL `http://localhost:8080/swagger-ui.html` if you are running the **invoicing-service** on `localhost` at port `8080`.

> [!NOTE] 
> In general the Swagger URL is `http://<SERVER_NAME>:<SERVER_PORT>/swagger-ui.html`.



## How to Run Application
**Start the application using these commands below**

> [!TIP] 
> Run these commands inside the root folder of this project; i.e inside the **invoicing-service** folder.


> [!IMPORTANT] 
> Create a jar file using `mvn clean install` command.


**Using maven (via command)** 
  ```
  mvn spring-boot:run
  ```

**From jar file**
  ```
  java -jar target/invoicing-service.jar
  ```

**From Eclipse**
- Set the **Maven Build** configuration in eclipse; select in base directory the *invoicing-service* workaspace, in the **goals** use `spring-boot:run`, and set the name (i.e. **invoicing-service run**). 
- To run in **debug mode**, set **Java Application** configuration in eclipse, browser the **invoicing-service** project and select with **search** bottom the `it.eng.dome.invoicing.service.InvoicingServiceApplication` class. 
Don't forget to add breakpoint for debugging.

 
**From Docker**
- Create the jar file following above compile instructions.
- Create manually Invoicing Service Docker image by running: `docker build . -t invoicing-service:X.Y.Z` (where `X.Y.Z` represents the version tag: i.e. 0.0.1).
- Run the Invoicing Service Docker image by executing: `docker run -d -p8080:8080 invoicing-service:X.Y.Z`
- If you want to give a name to the Invoicing Service Docker image by run it as: `docker run -d -p8080:8080 --name invoicing-service invoicing-service:X.Y.Z`

> [!NOTE]  
> By default spring boot application starts on port number 8080. If port 8080 is occupied in your system then you can change the port number by uncomment and updating the **server.port** property inside the **application.yaml** file that is available inside the **src > main > resources** folder.



## How to Run Unit Test Cases
**Run the test cases using this command below**

> [!TIP] 
> This command needs to run inside the root folder of this project i.e inside the **invoicing-service** folder

- **To run all the test cases**
  ```
  mvn test
  ```

