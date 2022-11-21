# AT&T XACML Demo

This sample illustrates the use of AT&T Xacml-3.0 to evaluate a XACML policy request passed through an http request. It shows how to set the policy used by AT&T Xacml-3.0, how to make a request to the PDP (Policy Decision Point), and how to parse the response.

# Prerequisites

Prerequisites known to be sufficient are
- Apache Maven 3.8.6
- Java version "11.0.16.1" 2022-08-18 LTS
- git version 2.30.0.windows.2 (other operating systems not tested)

# Download and Install AT&T XACML Demo

```bash
git clone https://github.com/att/xacml-3.0.git
cd xacml-3.0
mvn clean install
```

# Download and Build this Repository

This repository uses copies of policy and request files from the xacml-3.0 test. To avoid licensing issues, they are not included with this repository and need to be copied before building. As a prerequisite for the commands below, set `att_xacml_dir` to where xacml-3.0 was cloned.
```bash
git clone https://github.com/predrag3141/xacml-att-demo.git
${att_xacml_dir}/xacml-pdp/src/test/resources/testsets/conformance/xacml3.0-ct-v.0.4/IIA001Policy.xml src/main/resources/IIA001Policy.xml
${att_xacml_dir}/xacml-pdp/src/test/resources/testsets/conformance/xacml3.0-ct-v.0.4/IIA001Request.xml src/main/resources/IIA001Request.xml
mvn clean package
```

# Run the Sample

For your convenience, `run.sh` contains the same command as given below, and can be sourced (it is not meant to be run as an executable).
```bash
java -jar target/xacml.server-1.0-jar-with-dependencies.jar
```

While the jar is running, visit one of the following to get a policy decision
- For a `Permit` decision, leave the person requesting access as in `IIA001Request.xml`: http://localhost:8080/request
- For an `Indeterminate` decision, change the person requesting access to (for example) Joe: http://localhost:8080/request?who=joe

Changing the person requesting access appeears to make the policy inapplicable, though that could be confirmed by sifting through the debug output. A "TODO" would be to find a way to get a denial.

# Walkthorugh

The http request uses the `who` parameter in the URL to provide the name of someone that wants to read Bart Simpson's medical records at http://medico.com/record/patient/BartSimpson. If the parameter is missing, the request defaults to Julius Hibbert, the name in `IIA001Request.xml`. This happens because the sample reads `IIA001Request.xml` into a Request instance and only modifies the Request instance if the `who` parameter is set.

The policy file, `IIA001Policy.xml`, allows Julius Hibbert to read these medical records; for anyone else the policy decision is apparently `Indeterminate` (see the "TODO" in the section, "Run the Sample" to prove the policy can be caused to actually deny access).
