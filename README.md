# Gitlab Auto-Create And Assign MergeRequests

This Kotlin Script can be used in GitLab CI to create merge requests automatically and assign them to a developer for review based on a strategy(Currently it is using Queue).
For creating the merge requests it use the basic ideas from this [repo](https://github.com/tmaier/gitlab-auto-merge-request).
## Instructions

### 1) `CI_ACCESS_TOKEN`

First, you should create a Gitlab private access token and set it to a variable with the name `CI_ACCESS_TOKEN`. This is necessary for being able to open merge requests from CI.

### 2) Get and Set reviewers ID
You can retrieve the ID of project members with this call:
```bash
curl --header "PRIVATE-TOKEN: <your_access_token>" "https://<your_giltab_host>/api/v4/projects/<your_project_id>/members/all"
```
Notice that you should replace `<your_access_token>`, `<your_giltab_host>` and `<your_project_id>` with the correspond values.
After retrieving this list, You should create your reviewers Queue. You just need to set the IDs which can review the Merge Requests in variable `reviewerIDs`:

```Kotlin
val reviewerIDs = mutableListOf<String>("id1","id2")
```

Finally for this step you must set the first ID which should review to a variable named `CURRENT_REVIEWER_USER_ID` in Gitlab, This is necessary for assign the first merge requests, After doing this the script will handle the next reviewers itself.
### 3) Add your own labels for Merge Requests
You can add your own label for merge requests in method `getProperLabels`, Please make sure that you created that labels in Gitlab before. There is a default label named `Developer Review` in the script.

### 4) Prepare `.gitlab-ci.yml`
First you need to install the requirements for this script, It needs `sdkman` and `kscript` to run. For doing this you can add these lines to the `before_script` section:

```yaml
before_script:
  - curl -s "https://get.sdkman.io" | bash     # install sdkman
  - source "$HOME/.sdkman/bin/sdkman-init.sh"  # add sdkman to PATH
  - sdk install kotlin                        # install Kotlin
  - sdk install kscript                       # install Kscript 
```

You should add a stage to your CI for opening merge requests, For example you can create a stage named `openMergeRequest` like this:
```yaml
stages:
  - openMergeRequest
  - otherStage1
  - otherStage2
```

After doing this you should call this script in that stage, For example:

```yaml
stages:
  - openMergeRequest
  - otherStage1
  - otherStage2

Open Merge Request:
  stage: openMergeRequest
  script:
    - kscript GitlabAutoMRandAssign.kts # The name of the script
```

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License
[MIT](https://choosealicense.com/licenses/mit/)
