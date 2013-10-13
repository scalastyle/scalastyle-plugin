#! /bin/sh

eclipse_opt="-nosplash -consoleLog"
app="org.eclipse.equinox.p2.director"
update_site="file:/C:/code/scalastyle/scalastyle-plugin/org.scalastyle.scalastyleplugin.update-site/target/site/"
feature="org.scalastyle.scalastyleplugin.feature"

kepler()
{
	eclipse_dir="/c/code/eclipse/kepler/eclipse"
	
	eclipse_repo="http://download.eclipse.org/releases/kepler/"
	echo using kepler
}

juno()
{
	eclipse_dir="/c/code/eclipse/juno/eclipse"
	
	eclipse_repo="http://download.eclipse.org/releases/juno/"
	echo using juno
}

indigo()
{
	eclipse_dir="/c/code/eclipse/eclipse-jee-indigo-SR2-win32-x86_64/eclipse"
	
	eclipse_repo="http://download.eclipse.org/releases/indigo/"
	echo using indigo
}

helios()
{
	eclipse_dir="/c/code/eclipse/eclipse-jee-helios-SR2-win32-scala/eclipse"
	eclipse_repo="http://download.eclipse.org/releases/helios/"
	echo using helios
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
helios)	helios; shift;;
indigo) indigo; shift;;
juno) juno; shift;;
kepler) kepler; shift;;
*)	indigo;;
esac

case $1 in 
"" | "help")	usage ;;
"install") 		install ;;
"uninstall")	uninstall ;;
"update")		uninstall ; install ;;
*)				usage ;;
esac
