# Courier Android - Contribution Guidelines

[Courier Android][1] is an open-source project.
It is licensed using the [MIT License][2].
We appreciate pull requests; here are our guidelines:

1. [File an issue][3]
    (if there isn't one already). If your patch
    is going to be large it might be a good idea to get the
    discussion started early.  We are happy to discuss it in a
    new issue beforehand, and you can always email
    <foss+tech@go-jek.com> about future work.

2. Please also make sure your code compiles by running `./gradlew assembleDebug`

3. If you want to test locally you can publish artifacts locally using `./scripts/publishMavenLocal.sh` 

4. DO follow our coding style (as described below).

5. DO run spotlessApply task (`./gradlew spotlessApply`) before submitting a pull request

6. DO include tests when adding new features. When fixing bugs, start with adding a test that highlights how the current behavior is broken.

7. We use the [binary-compatibility-validator plugin][9] for tracking the binary compatibility of the APIs we ship. If your change implies changes to any public API, run `./gradlew apiDump` to generate the updated API dumps and commit those changes.

8. We ask that you squash all the commits together before
    pushing and that your commit message references the bug.

9. DON'T surprise us with big pull requests. Instead, file an issue and start a discussion so we can agree on a direction before you invest a large amount of time.

## Coding Style

The coding style employed here [Kotlin Coding Conventions][4].

We use [Spotless][8] with ktlint for Kotlin code formatting. To make sure the IDE agrees with rules we use, please run `./gradlew ktlintApplyToIdea` to generate IntelliJ IDEA / Android Studio Kotlin style files in the project .idea/ folder.

## Issue Reporting
- Check that the issue has not already been reported.
- Be clear, concise and precise in your description of the problem.
- Open an issue with a descriptive title and a summary in grammatically correct,
  complete sentences.
- Include any relevant code to the issue summary.

## Pull Requests
- Please read this [how to GitHub][5] blog post.
- Use a topic branch to easily amend a pull request later, if necessary.
- Write [good commit messages][6].
- Use the same coding conventions as the rest of the project.
- Open a [pull request][7] that relates to *only* one subject with a clear title
  and description in grammatically correct, complete sentences.

Much Thanks! ❤❤❤

GO-JEK Tech

[1]: https://github.com/gojek/courier-android
[2]: https://opensource.org/licenses/MIT
[3]: https://github.com/gojek/courier-android/issues
[4]: https://kotlinlang.org/docs/coding-conventions.html
[5]: http://gun.io/blog/how-to-github-fork-branch-and-pull-request
[6]: http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html
[7]: https://help.github.com/articles/using-pull-requests
[8]: https://github.com/diffplug/spotless
[9]: https://github.com/Kotlin/binary-compatibility-validator