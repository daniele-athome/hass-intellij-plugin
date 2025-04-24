# Contributing

First of all, thanks for taking the time to contribute.

## What can I do?

* [Report bugs](https://github.com/daniele-athome/hass-intellij-plugin/issues/new?template=bug_report.yml)
* Fix bugs
* Improve existing features

## IntelliJ plugin development

> This is required if you want to fix bugs or improve existing features.

If you are not fluent with IntelliJ plugin development, you should start by reading
[their documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html). It is not perfect - IntelliJ is a huge
project - but it really helps giving a big picture and a straight track to follow. Much of your learning will also come
from browsing the [old forums](https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development) and the [new forums](https://platform.jetbrains.com/), diving through other plugins and IntelliJ platform source
code, trials and errors, and a lot of time.

## Dive into the code first

Explore the existing code before trying to modify it. Use [good first issues](https://github.com/daniele-athome/hass-intellij-plugin/issues?q=is%3Aissue%20state%3Aopen%20label%3A%22good%20first%20issue%22)
to get familiar with the codebase.

## Fix bugs / Improve existing features

Please look at the existing code before writing anything and try to follow some simple rules:

1. Look for already existing code (in either the plugin or IntellJ SDK) before writing what you need
2. Try to put the code in the right package using your best judgement
3. Java is not allowed, only Kotlin please
4. I know it's boring, but please _please_ write tests :smiling_face_with_three_hearts:
5. Prefer fast but memory-hungry over slow and low memory footprint: IDEs need to be fast and sometimes that might need
   higher memory usage (i.e. use caches)

## Contribute new features

Before implementing new features (or major changes to existing ones), please open an issue to discuss it with me.
