# Home Assistant IntelliJ Plugin

# :warning: :radioactive: WARNING :radioactive: :warning:

**This is a work in progress and ~~will~~ may eat all your files.**

* this is a plugin for working with Home Assistant configuration files in IntelliJ IDEs
* the code has been mainly tested by me (I use it, of course), but there are currently very few automated tests
* there are still some (minor) performance issues
* please be patient as ~~I don't really know what I'm doing since~~ this is my first IntelliJ plugin
* plugin UX is still PoC-level

---

<!-- Plugin description -->
Support for [Home Assistant](https://www.home-assistant.io/) configuration files.

* Bundled YAML schema from the awesome [Visual Studio Code Extension](https://github.com/keesschollaart81/vscode-home-assistant/)
* Go to definition for any locally defined script, automation, `input_*`, group, `shell_command`, `rest_command`, `timer`
* Find usages of any of the above
* Access remote Home Assistant data about available services (actions) and entities to resolve references
* Action call completion (only in `action:` and `service:` contexts)
* Entity completion (only in `entity_id:` contexts)
* Secret completion
* Go to secret value
* Dedicated module facet, supporting multiple Home Assistant configurations in the same project (only for IDEA)
* Provide documentation hints for supported elements (experimental)
* [Material Design Icons](https://materialdesignicons.com/) completion

<!-- Plugin description end -->

## Notice

Home Assistant configuration layouts can vary widely among users. However, for performance reasons (and for the sake of
my sanity), some use cases are not supported:

#### Code insights driven by includes

I might implement this one day, but the issue is that YAML code can be included in multiple ways:
`!include` and related directives, YAML anchors, `lovelace_gen`, some other unknown custom integration...  
Because of this complexity, the plugin considers all YAML files in the module as Home Assistant configuration files.

#### Integrations not as top-level YAML keys

Due to includes (see above), you could write something like this:

```yaml
# in configuration.yml
script: !include scripts.yml
```

```yaml
# in scripts.yml
script_name:
  sequence:
    [...]
```

Since there are so many possible ways to structure this, it is not supported (and probably never will be).
The only supported method is to always have integrations at the top level and use
[packages](https://www.home-assistant.io/docs/configuration/packages/).

```yaml
# in configuration.yml
homeassistant:
  # you can use any packages notation, but every YAML file must have integrations at the top level.
  packages: !include_dir_named packages
```

```yaml
# in packages/scripts.yml
# every YAML file must have integrations (in this case, "script") at the top level.
script:
  script_name:
    sequence:
      [...]
```
