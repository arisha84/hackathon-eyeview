from boto.utils import ShellCommand
import deploy_tools

def deploy():

    deploy_tools.install_geoIP_databases()
    deploy_tools.install_memcache()
    deploy_tools.install_upstart_script("cep-manager")

    #DISABLE SWAP!!!
    ShellCommand("sudo swapoff -a")