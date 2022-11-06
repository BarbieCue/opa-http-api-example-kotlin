# Open Policy Agent HTTP API example

This is a minimalistic demonstration of 
restricting an HTTP API with OPA.


## Run

1. [Download and install](https://www.openpolicyagent.org/docs/v0.11.0/get-started/) the Open Policy Agent. *Tip: drop the opa executable to ~/.local/bin, so it is on the PATH.*
2. Build the example policy and move the result to the right folder  
    ```sh
    cd opa
    opa build example-policy.rego
    mkdir bundles
    mv bundle.tar.gz bundles
    ```
3. Start the OPA server (+ bundle server)
    ```
    docker-compose up
    ```
4. Start the API server (kotlin application)


# What happens here?

![](opa/opa_flow.png)

On each HTTP request to the API server, 
the API server asks the OPA server whether the requesting client
is allowed to access the document or not.

But why does the OPA server knows who is allowed to do what?
From the policy definition file `example-policy.rego`.

![](opa/opa_build.png)

In order to re-define what is allowed and what is forbidden,
edit the policy and rebuild it ("Run", Step 2).


## Example requests to the API server (being a client)

```
allowed:
curl --user betty:password localhost:8080/finance/salary/betty

forbidden:
curl --user betty:password localhost:8080/finance/salary/alice
```


# References

- Inspired by https://www.openpolicyagent.org/docs/latest/http-api-authorization/
- Corresponding python implementaion https://github.com/open-policy-agent/contrib/tree/main/api_authz
