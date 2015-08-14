lein-rpm
========

creates rpms for a lein project


## Usage

add ```[fatrpm "0.1.0-SNAPSHOT"]``` to the plugins tag e.g in project.clj

e.g ```:plugins [[fatrpm "0.1.0-SNAPSHOT"]]```

add an ```:rpm``` key and map with ```:name, :summary, :copyright, :workarea, :username, :groupname and :mappings ```
to the project.clj file.


Type in ```lein fatrpm``` from the command line,  
note that any mappings not found will be flagged as an error and the build will fail.


## Example rpm configuration

```
  :rpm {:name      "hdfsimport"
        :summary   "hdfs import"
        :copyright "mycopyright"
        :workarea  "target"
        :username  "hdfs"
        :groupname "hdfs"
        :mappings  [; Jar
                    {:directory "/opt/hdfsimport/lib"
                     :filemode  "644"
                     :sources   [[(str "target/hdfsimport-"
                                    "0.1.0-SNAPSHOT"
                                    "-standalone.jar")
                                  "hdfsimport.jar"]]}


                    ; Log dir
                    {:directory           "/var/log/hdfsimport"
                     :filemode            "755"
                     :directory-included? true}

                    ; bin files
                    {:directory           "/opt/hdfsimport/bin"
                     :filemode            "755"
                     :directory-included? true
                     :sources             ["src/resources/bin/hdfsimport.sh"
                                           "src/resources/bin/logToHdfsSync.sh"]}

                    ; Config dir
                    {:directory           "/opt/hdfsimport/conf"
                     :filemode            "755"
                     :directory-included? true}

                    ; Config file
                    {:directory     "/opt/hdfsimport/conf"
                     :filemode      "644"
                     :configuration true
                     :sources       ["src/resources/conf/hdfsimport.edn"
                                     "src/resources/conf/log4j.properties"]}

                    ; Default file
                    {:directory     "/etc/sysconfig"
                     :filemode      "644"
                     :configuration true
                     :sources       [["src/resources/pkg/sysconfig" "hdfsimport"]]}

                    ; Init script
                    {:directory "/etc/init.d"
                     :filemode  "755"
                     :username  "root"
                     :groupname "root"
                     :sources   [["src/resources/pkg/init.sh" "hdfsimport"]]}]}
```

## Building RPMS using Mac/Windows

You will need to use https://www.vagrantup.com:

An example Vagrantfile to do this is:

```
# -*- mode: ruby -*-
# vi: set ft=ruby :

# Vagrantfile API/syntax version. Don't touch unless you know what you're doing!
VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  config.vm.box = "centos"
  config.vm.box_url = "https://github.com/2creatives/vagrant-centos/releases/download/v6.5.3/centos65-x86_64-20140116.box"
end
```
