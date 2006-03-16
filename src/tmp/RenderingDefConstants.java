/*
 * omeis.providers.re.metadata.RenderingDef
 *
 *------------------------------------------------------------------------------
 *
 *  Copyright (C) 2004 Open Microscopy Environment
 *      Massachusetts Institute of Technology,
 *      National Institutes of Health,
 *      University of Dundee
 *
 *
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation; either
 *    version 2.1 of the License, or (at your option) any later version.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public
 *    License along with this library; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *------------------------------------------------------------------------------
 */

package tmp;

import java.util.HashMap;
import java.util.Map;

import ome.model.enums.RenderingModel;


public class RenderingDefConstants
{

	/** GreyScale model. */
	public static final int 	GS = 0;
	
	/** RGB model. */
	public static final int 	RGB = 1;
	
	/** HSB model. */
	public static final int 	HSB = 2;
	
    /** Human readable model. */
    private static String[] models;
    static {
        models = new String[3];
        models[GS] = "greyscale";
        models[RGB] = "rgb";
        models[HSB] = "hsb";
    }
    
    private static Map Models = new HashMap();
    static {
        Models.put("greyscale",Integer.valueOf(GS));
        Models.put("rgb",Integer.valueOf(RGB));
        Models.put("hsb",Integer.valueOf(HSB));
    }
    public static int convertType(RenderingModel type)
    {
        return ((Integer) Models.get(type.getValue())).intValue();
    }
    
    public static RenderingModel convertToType(int model)
    {
        RenderingModel type = new RenderingModel();
        type.setValue(models[model]);
        // FIXME 
        return type;
    }
    
}
