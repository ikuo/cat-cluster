## Running

```
$ eb init
$ eb create -ip eb-multicontainer-role
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
  instance_profile: eb-multicontainer-role
  platform_name: null
  platform_version: null
  profile: null
  repository: null
  sc: git
  workspace_type: Application
```
