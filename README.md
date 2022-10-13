
# Enigmafeeder

![Logo](https://raw.githubusercontent.com/kataroot/enigmafeeder/master/logo/logo.png)

Tired of creating by hand random numbers to create your Enigma machine seeds? Enigmafeeder is an API REST designed specially for this task.

## Authors

- [@kataroot](https://www.github.com/kataroot)

## Software Requirements

The following software is required to deploy Enigmafeeder successfully:
- Docker (or any other container builder tool like podman, buildah, etc.)
- Kubectl
- Helm v3
- Git

It is also necessary to have access to a Kubernetes cluster.

## Notes

The build process runs completely inside a container. This way, the process is isolated and less prone to errors.

This is the first Spring Boot application I develop, so this part could certainly be. The tests are not as detailed as I would like it to be, and the exception handling should be more fine-grained. 

About the Kubernetes deployment and the use of the default namespace, I could have created a new namespace and a service account with Helm, but these are resources that should be created by the Kubernetes administration team. 

This project assumes that we don't have a CI/CD tool to create a proper pipeline, so the installation process requires to perform some manual tasks and changes.

### About Helm charts

The Helm chart creates the following resources:

- **Deployment**. The Deployment is responsible for running the application pods, one for each replica requested, and automatically replaces any failing one. This application is presumably not going to have a lot of traffic, and it is not a heavy process either, so the default replica number is 1. Otherwise, we could ask for 2 or 3 replicas or even use an HPA (HorizontalPodAutoscaler) resource to scale the application dynamically.

- **Service (ClusterIP)**. This resource balances traffic between the pods group and creates an interface to it, exposing the application service inside the cluster.

- **Ingress**. An Ingress resource provides routing rules to manage external users' access to the services. It is also used to terminate SSL/TLS connections. This resource is used by Enigmafeeder only on non-production environments.

#### Monitoring

The following liveness and readiness configuration is used by Enigmafeeder:
```
Liveness : GET /actuator/health/liveness
Readiness: GET /actuator/health/readiness
```

These endpoints are automatically created by the *actuator* Spring Boot plugin. Also, Enigmafeeder is very simple and does not depend on external resources like databases or message queues, so none of them requires further configuration.

## Prepare the environment

#### Install Ingress

In a real environment, Ingress should not be installed by hand using an out-of-the-box Helm package. This is not IaC compliant at all. Ingress should have its own repository and the namespace must be created by the Kubernetes administration group, assigning it the necessary policies.

If you have a local K8s cluster and you need to install an Ingress on it, you can use the following command:

```
helm upgrade --install ingress-nginx ingress-nginx \
  --repo https://kubernetes.github.io/ingress-nginx \
  --namespace ingress-nginx --create-namespace
```

#### Create an autosigned SSL/TLS certificate (only for non-production deployments)

Again, this is wrong and should not be done in a corporate cluster. We are going to manually create a certificate and its corresponding secret in K8s. The idea of creating a YAML file and adding it to the repo is not better, because we are adding a private key in almost clear text. A better option is to have a Vault configured as a Certificate Manager for our cluster, for example. Anyway, the chosen option is enough to test SSL connections in non-production environments.

Create the crt and key files with `openssl`:

```
openssl req -subj "/C=ES/ST=Malaga/L=Malaga/O=The Workshop/OU=Org/CN=enigmafeeder.stg.tws.local" -x509 -newkey rsa:4096 -nodes -keyout enigmafeeder.key -out enigmafeeder.crt -sha256 -days 365
```

Note that `enigmafeeder.stg.tws.local` will be the app FQDN. It is neccesary to add it to your `/etc/hosts` pointing to 127.0.0.1.

#### Create the certificate secret in the cluster

```
kubectl create secret tls enigmafeeder-tls --namespace default --key enigmafeeder.key --cert enigmafeeder.crt
```

## Build The Project

> **PLEASE NOTE:** We assume you are building and deploying the first version (1.0.0) of Enigmafeeder. If not, please change the version references in the following commands.

- Clone this repository into your local PC:

```bash
  git clone https://github.com/kataroot/enigmafeeder.git
  cd enigmafeeder
```

- Build the image. This steps compiles and executes the tests from inside a container. You have to run these commands from the project root directory.
 
  - Build with Docker

  ```bash
  docker build --build-arg VERSION=1.0.0 -t tws/enigmafeeder:1.0.0 -f docker/Dockerfile .
  ```

  - Build with Podman

  ```bash
  podman build --build-arg VERSION=1.0.0 -t tws/enigmafeeder:1.0.0 -f docker/Dockerfile .
  ```

  - Build with Buildah

  ```bash
  buildah bud --build-arg VERSION=1.0.0 -t tws/enigmafeeder:1.0.0 -f docker/Dockerfile .
  ```

## Running Tests

#### Run the Spring Boot tests

The Spring Boot tests are executed automatically at build time, so you don't have to worry for them. If any test goes wrong, the Docker image is not built.

#### Run Helm test

Should you modify any Helm file, you must test the charts using the following commands. Like the Docker commands, you have to run these ones from the project root directory.

- Test a modified values.yaml

```bash  
  helm lint -f helm/values-nonproduction.yaml helm
  helm lint -f helm/values-production.yaml helm
```

- Test the templates. This command renders chart templates locally and displays the output:

```bash
  helm template -f helm/values-nonproduction.yaml helm 
  helm template -f helm/values-production.yaml helm 
```

- Do a dry-run and debug the chart installation:


```bash
helm upgrade enigmafeeder --install --debug --dry-run -f helm/values-nonproduction.yaml helm
helm upgrade enigmafeeder --install --debug --dry-run -f helm/values-production.yaml helm
```

## Deployment

Once the build phase has been done, you can use Helm to install the application in your cluster.

### Non-production environment

```bash
helm upgrade enigmafeeder --install -f helm/values-nonproduction.yaml helm
```

### Production environment

```bash
helm upgrade enigmafeeder --install -f helm/values-production.yaml helm
```

## Usage/Examples

Enigmafeeder only exposes one endpoint:

**/randomizer**

Parameters:

- *howmany*: How many numbers do you want to create.
- *min*: Minimum value for the random numbers.
- *max*: Maximum value for the random numbers.

Output:

A JSON object with a *numbers* array containing the random numbers requested.
```
{"numbers":[n1, n2, ... , nx]}
```

Example:

You can test the application in a non-production deployment:

```bash
curl -k "https://enigmafeeder.stg.tws.local/randomizer?howmany=7&min=1000&max=2000"

{"numbers":[1402,1650,1807,1280,1802,1940,1146]}

```

