# Working example of combination Arquillian, Graphene, Drone and Warp
This project demonstrates the working of the integration testing framework Arquillian with its extensions Graphene, Drone and Warp. 
The real interesting thing is the EJB which is mocked by Mockito and can be fully controlled during the Warp test run.

The source code is documented so you should easily understand how the parts work together.

# Mocking EJBs
Before deployment into the integration testing environment all EJBs to be deployed are transformed by Javassist. Javassist removes all direct dependencies, for example to underlying repository classes or services. You must still include return types of the serivce methods.
The EJB acts only as a facade. Inside the facade exists a Mockito instance which can be fully controlled by your assignments.

# Tested environment
* JBoss 7.1.1, should work in major/minor versions tooo
* Firefox
* Java 1.7; won't work in 1.6 due to Arquillian dependency to the JRE method getLoopbackAddress() which is available since 1.7

# Running the example
Import the project into your Eclipse instance, deploy the application to your JBoss. You should see the text "A real database user".
Execute the unit test inside Eclipse. Firefox opens and show "A mocked user instance". The UserRepository class is not deployed. The UserService returns the defined value inside the test.
