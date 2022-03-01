package de.unileipzig.irpsim.gams.helper;

import java.io.File;
import java.util.Arrays;

import com.gams.api.GAMSDatabase;
import com.gams.api.GAMSGlobals.DebugLevel;
import com.gams.api.GAMSParameter;
import com.gams.api.GAMSParameterRecord;
import com.gams.api.GAMSWorkspace;
import com.gams.api.GAMSWorkspaceInfo;

public class ShowGDXValues {
   public static void main(String[] args) {
      File file = new File(args[0]);

      final GAMSWorkspaceInfo wsInfo = new GAMSWorkspaceInfo();
      wsInfo.setWorkingDirectory(file.getParentFile().getAbsolutePath());

      GAMSWorkspace workspace = new GAMSWorkspace(wsInfo);
      wsInfo.setDebugLevel(DebugLevel.KEEP_FILES);
      GAMSDatabase parameterDB = workspace.addDatabaseFromGDX(file.getName());

      // for (Object o : parameterDB) {
      // System.out.println(o);
      // }

      // parameterDB.getSet("load_CL1");

      // parameterDB.getVariable("par_L_DS_CL");

      GAMSParameter par = parameterDB.getParameter("par_L_DS_CL");

      for (GAMSParameterRecord o : par) {
         if (o.getValue() != 8.512)
            System.out.println(Arrays.toString(o.getKeys()) + " " + o.getValue());
      }
   }
}
