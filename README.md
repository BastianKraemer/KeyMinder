#KeyMinder
KeyMinder is a connection and login credential management program. Originally it has been created to handle multiple SSH connections to various hosts in an easy way.  Therefore KeyMinder just starts an application with some custom command line arguments, for example "PuTTY". To be more flexible, the command line interface of each application is configured by a "XML application profile", so you can launch whatever you want.
Consequently you can store all your login credentials for any host in a clear hierarchical structure and directly connect to them using "PuTTY", "WinSCP" or another application – all with the same data.
All your data will be saved in a single file, which can be encrypted with AES-256.

This sounds a bit technical, but KeyMinder is not just for IT experts, many features are part of optional "plugins". If you don’t want to use those "SSH-Tools", just disable the plugin.

**Note: This is only the first release of this project. The whole application is still in development.**

##Getting started
KeyMinder is written in Java, so it can be used on Linux and Windows. The graphical user interface is realized with JavaFX.
To start KeyMinder on your computer you will need a Java 8 installation on your system.

> If you are using the OpenJDK Java Platform it's possible that the the JavaFX runtime environment is not included in your installation.
> In this case you will have to setup the JavaFX runtime environment at first.

##Security
KeyMinder is able to encrypt your login credentials with AES-256 (and PBKDF2-Hashes). For more information take a look at the **MANUAL.md** file.

##Contributing
There are several possibilities to contribute something to this project. 
You can report bugs or offer ideas for new features or other improvements. Another possibility for contributing is to share your own application profiles (see plugin "SSH-Tools"), but you can even implement new features by yourself.

##Build
You can build KeyMinder using Maven. Just run `mvn package` to build the jar file.

##To-Do
As already mentioned, this is only the first release of this project. 
Here’s a list of some features or improvements that will be implemented during the next time:
- The whole topic FXML has been completely ignored until now; in consequence KeyMinder doesn’t really match the MVC pattern at the moment. This will be changed in future versions.
- The documentation of this project is intermittent at the moment. I will try to change this step by step.
- Many other new features, for example a password generator
- …

##License
KeyMinder is released under GNU GPL v3 License
