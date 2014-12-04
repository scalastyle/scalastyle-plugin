#! /bin/sh

eclipse_opt="-nosplash -consoleLog"
app="org.eclipse.equinox.p2.director"
update_site="file:/home/mfarwell/code/scalastyle/scalastyle-plugin/org.scalastyle.scalastyleplugin.update-site/target/site/"
feature="org.scalastyle.scalastyleplugin.feature"

kepler()
{
	eclipse_dir="/home/mfarwell/dev/eclipse"
	
	eclipse_repo="http://download.eclipse.org/releases/kepler/"
	echo using kepler
}

luna()
{
	eclipse_dir="/home/mfarwell/dev/luna"
	
	eclipse_repo="http://download.eclipse.org/releases/luna/"
	echo using luna
}

usage()
{
    echo $0 "<help|install|uninstall|update>"
	exit 2
}

install()
{
	echo installing
	$eclipse_dir/eclipse $eclipse_opt -application $app -repository "$update_site" -installIU "$feature.feature.group"
	ls -ld $eclipse_dir/features/org.scalastyle.scalastyleplugin.feature*
	ls -l $eclipse_dir/features/org.scalastyle.scalastyleplugin.feature*
	ls -l $eclipse_dir/plugins/org.scalastyle.scalastyleplugin*
	echo done
}

uninstall()
{
	echo uninstalling
	$eclipse_dir/eclipse $eclipse_opt -application $app -repository $eclipse_repo -uninstallIU "$feature.feature.group"
	ls -l $eclipse_dir/features/org.scalastyle.scalastyleplugin.feature*/feature.xml
	$eclipse_dir/eclipse $eclipse_opt -application org.eclipse.equinox.p2.garbagecollector.application -profile epp.package.jee
	ls -l $eclipse_dir/plugins/org.scalastyle.scalastyleplugin*
	echo done
}

case $1 in
luna)	luna; shift;;
kepler) kepler; shift;;
*)	luna;;
esac

case $1 in 
"" | "help")	usage ;;
"install") 	install ;;
"uninstall")	uninstall ;;
"update")	uninstall ; install ;;
*)		usage ;;
esac
