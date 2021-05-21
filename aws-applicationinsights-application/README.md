# AWS::ApplicationInsights::Application

Congratulations on starting development! Next steps:

1. Write the JSON schema describing your resource, `aws-applicationinsights-application.json`
1. Implement your resource handlers.

The RPDK will automatically generate the correct resource model from the schema whenever the project is built via Maven. You can also do this manually with the following command: `cfn generate`.

> Please don't modify files under `target/generated-sources/rpdk`, as they will be automatically overwritten.

The code uses [Lombok](https://projectlombok.org/), and [you may have to install IDE integrations](https://projectlombok.org/setup/overview) to enable auto-complete for Lombok-annotated classes.

## Running Contract Tests
### Prerequisites
* Requires `samcli` and `cfncli` to be usable
    * Its recommended to use python virtual environments to do this
    * In this directory, run `python3 -m venv venv`
    * Run `source venv/bin/activate`
    * Run `pip3 install cloudformation-cli aws-sam-cli cloudformation-cli-java-plugin`
* Create a resource group called "contract-test-resource-group" in your test account

### Steps
* First, build the project
* In one terminal:
  * Run `sam local start-lambda`
* In another terminal:
  * Paste in aws env vars for your test account. Make sure to set `AWS_DEFAULT_REGION`
  * Then run `cfn test` to execute the tests
