eclipse_dir="/d/utils/eclipse/eclipse-jee-helios-SR2-win32-scala/eclipse"
eclipse_opt="-nosplash -consoleLog"
app="org.eclipse.equinox.p2.director"
update_site="file:/D:/code/scalastyle/scalastyle-plugin/org.scalastyle.scalastyleplugin.update-site/target/site/"
eclipse_repo="http://download.eclipse.org/releases/helios/"
feature="org.scalastyle.scalastyleplugin.feature"

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
	echo done
}

uninstall()
{
	echo uninstalling
	$eclipse_dir/eclipse $eclipse_opt -application $app -repository $eclipse_repo -uninstallIU "$feature.feature.group"
	ls -l $eclipse_dir/features/org.scalastyle.scalastyleplugin.feature*/feature.xml
	echo done
}

case $1 in 
"" | "help")	usage ;;
"install") 		install ;;
"uninstall")	uninstall ;;
"update")		uninstall ; install ;;
*)				usage ;;
esac
