#include <stdio.h>
#include <string.h>
int main(int argc,char **argv)
{
  if(strrchr(argv[0],'/') != NULL)
    argv[0] = strrchr(argv[0],'/')+1;
  if(strcmp(argv[0],"mkbootimg") == 0)
    mkbootimg_main(argc,argv);
  else if(strcmp(argv[0],"exbootimg") == 0)
    exbootimg_main(argc,argv);
  else if(strcmp(argv[0],"revise") == 0)
    revise_main(argc,argv);
  else
    return 0;
}
