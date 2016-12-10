KeyMinder - Manual
==================

KeyMinder is a connection and login credential management program - other people may call it a "Password Manager".

This document is a short manual about some KeyMinder features.
You won't find any information like "How to create a password file" in this document.
If you need (or want to have) explanations like this, then this application is most likely not the right one for you :).

This manual only contains some brief introduction how you can use the absolutely not self-explanatory features.

Encryption
----------

At first you may want to know something about the encryption of KeyMinder.
The supported encryption methods are: (cipher/hash algorithm)

- AES-128/MD5		(worst)
- AES-256/SHA-256
- AES-256/PBKDF2	(best)

By default Java disables the encryption with AES-256 because of some US export restrictions.
You can only enable encryption with AES-256 by "upgrading" your Java installation using the "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files".
You will find them on the Java SE download page.

Settings 
--------

All your preferences will be saved in an XML file called _keyminder_settings.xml_.
By default KeyMinder will take a look at your working directory to find a settings file, after this users home directory will be checked.

The settings file is not encrypted.

> All settings which are defined in the "File settings" will be saved in the password file itself, not in the KeyMinder settings file.


Console Mode
-------------

KeyMinder includes two user interfaces. By default the graphical user interface will be used, but there is an interactive console mode too.
You can force KeyMinder to use the "ConsoleMode" using the command line switch `--console`.
For more information about the command line take a look at the "Command line options" section below.

You can also access the interactive console using the graphical user interface:
Just open the "Terminal" (Tools -> Terminal) and you are able to customize KeyMinder with some even more special features ;)

To get started with the _ConsoleMode_ try the command `help`. This will print a list of all currently available commands.
If you want to learn more about a specific command, type `man <command>` to get a brief introduction.

Command line options
--------------------

All options can be used with `--`, `-` or `/`.

###### Open a password file on startup

	--open [file] [-pw [password]]
	 
###### Load another settings file

	--settings-file [file]

###### Start in console mode (no graphical user interface)

	--console

###### Tell KeyMinder to be a bit more verbose

	--verbose
	
> Most likely you have to run KeyMinder using 'java -jar' to run in console mode. For example: `java -jar KeyMinder.jar --console`

###### Suppress some standard console output

	--silent

###### Disable output redirect to the "Terminal"

	--no-output-redirect

###### Print the current application version

	--version

###### Print a short help

	--help

KeyMinder Plugins
-----------------

You can extend KeyMinder with some custom plugins. Each plugin is loaded by the Java Service loader, so you can easily extend KeyMinder with custom plugins by adding them to the Class-Path.
Every KeyMinder plugin needs to be enabled at first, this can be done via the KeyMinder settings dialog or the `plugins` command.

### SSH-Tools plugin

To use this features the plugin "SSH-Tools" must be enabled.

##### Configure host port and login credentials

You can use each tree node for a single SSH configuration. Therefore the side bar contains a tab page called "SSH".


##### Command line generator

KeyMinder SSH-Tools plugin allows you to start applications using their command line interface and the data you have stored in your password file.
Therefore you can pass any required login credentials directly to another application.

Every launchable application is defined in an XML file called _command line descriptor_.
The logic how the command line argument are generated has to be defined in a separate JavaScript file.

Currently KeyMinder includes _command line descriptor_ files for the applications "PuTTY" and "WinSCP".
These built-in descriptors allows you to handle socks connections as well as launching "PuTTY" or "WinSCP" using the context menu.

All built-in descriptors are disabled by default. You can enable them using the settings dialog: Edit -> Preferences -> SSH-Tools -> Features
	
Or even using ConsoleMode or Terminal with one of these commands:

	config -s sshtools.enable_putty yes
	config -s sshtools.enable_winscp yes

After a restart of KeyMinder you'll find new items in the context menu.

> Don't forget to define the path to PuTTY and WinSCP. This configuration can be done in the settings dialog or using ConsoleMode/Terminal with one of these commands:
>
>	config -s sshtools.puttypath "<path>"
>	config -s sshtools.plinkpath "<path>"
>	config -s sshtools.winscppath "<path>"
> 
> Don't forget to save the new configuration using `config --save`.


##### SSH port forwarding

If you want to use port forwarding, you have to use the following pattern:

	[source port]:[host name]:[host port]

To display all forwarding more clearly you have to use multiple lines.
It is necessary to store one forward in each line. It's also highly recommended to avoid having empty lines between your forwarding.
	
Maybe your configuration could look like this:

	80:example.com:80
	443:example.com:443

> Every line in this text area that starts with "#" will be ignored.


##### Start and stop socks profiles

You can handle several SSH socks connection using KeyMinder, but you have to know that this program just starts other applications for this goal.
Currently you can establish socks connections using the SSH applications "PuTTY" and "Plink". Both applications will exactly do the same, but "Plink" won't show a window on your desktop.

> Download page for "PuTTY" and "Plink": http://www.chiark.greenend.org.uk/~sgtatham/putty/download.html

You can create socks profiles with a graphical dialog in the SSH-Tools socks configuration.

After this you can launch your profiles using the menu item "Socks" or via ConsoleMode/Terminal with the command `socks [start|stop] [name]`.
For more information about this command use `man socks`.


##### The "Using socks" feature

Currently this feature is only available if you are using _PuTTY_.
When you have started a socks profile, you can tell _PuTTY_ to connect to a server using this socks connection.

Unfortunately I don't know a way to do this via the command line, but there is a workaround:

1. Start PuTTY, select "Connection" -> "Proxy"
1. Set the following configuration parameters:
	- Proxy type: **SOCKS5**
	- Proxy hostname: **localhost**
	- Proxy port: **{The port for dynamic forwarding of your socks profile}**
1. Go back to "Session" and save your session
1. Open the KeyMinder socks configuration again and add the following line to the "Additional parameters to use socks connections" text area: `putty_sessionname=<your session name>`

> Every line in this text area that starts with "#" will be ignored.

Now you can start SSH connections using your socks profile.


##### Create own command line descriptors

There are at least a hundred other applications with a command line interface that could be launched using KeyMinder.

This manual won't explain how it exactly works, in my opinion the best way to create your own application profile is to copy one of the built-in XML files and adjust it to your own requirements.
Feel free to modify them the way you want.

> You will find the built-in _command line descriptors_ in the main resources of the SSH-Tools plugin project.

The last step to include your own profiles into KeyMinder is very simple: Just put them in a folder and tell KeyMinder the location of this folder, by default this path is './sshtools'.

You can define this path in the 'path configuration' of the SSH-Tools plugin (take a look at: SSH-Tools settings) or by using the config command: `config -s sshtools.cmdlinedescriptors.path [path]`.

### KeyClip plugin

The KeyClip plugin allows you to transfer the user name and password from the KeyMinder Sidebar directly to any other application.
By default the KeyClip plugin copies the user name into your clip board and displays an icon in your task bar.
After a click on this task bar icon the password will be copied into the clip board.

Alternatively, you can configure the KeyClip plugin to run a custom application instead of copying user name and password to the clip board.
This can be done using the KeyMinder Shell.

##### Configure an external application:

To configure an external application that should be launched you have to define the KeyMinder configuration setting "keyclip.command" with a string of the following pattern: `{program path} {argument 1} {argument 2} {argument n}`.

This can be done by using the shell command `config`. Maybe your command could look like this:

	config --set keyclip.command "/path/to/any/application --user \"\${keyclip.user}\" --pw \"\${keyclip.pw}\""

To disable this feature you can use

	config --delete keyclip.command

Alias mapping
-------------

If you like the command line, then the KeyMinder ConsoleMode/Terminal is maybe your favorite UI.

To customize the KeyMinder shell, you can introduce a file with alias definitions called _keyminder_alias.conf"_ which must be located next to your jar file or in the users home directory.

The file should look like this:

```
# This is a comment
alias_name = alias command
hello = echo "Hello world"
```

