## Running

```
sbt> re-start
```

## Deploying

```
$ eb init
$ eb create -i t2.micro -ip aws-elasticbeanstalk-ec2-role
```

## Example of config.yml

Put the following as `.elasticbeanstalk/config.yml`

```
branch-defaults:
  default:
    environment: null
    group_suffix: null
  master:
    environment: cat-cluster-dev
global:
  application_name: cat-cluster
  branch: null
  default_ec2_keyname: yubikey9a
  default_platform: multi-container-docker-1.12.6-(generic)
  default_region: ap-northeast-1
  instance_profile: aws-elasticbeanstalk-ec2-role
  platform_name: null
  platform_version: null
  profile: null
  repository: null
  sc: git
  workspace_type: Application
```
