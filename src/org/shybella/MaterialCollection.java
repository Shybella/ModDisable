/*
    TekkitCustomizer Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.shybella;

import java.util.ArrayList;

//ordered list of material info objects, for fast searching
public class MaterialCollection
{
	ArrayList<MaterialInfo> materials = new ArrayList<MaterialInfo>();
	
	void Add(MaterialInfo material)
	{
		int i;
		for(i = 0; i < this.materials.size() && this.materials.get(i).typeID <= material.typeID; i++);
		this.materials.add(i, material);
	}
	
	//returns a MaterialInfo complete with the friendly material name from the config file
	MaterialInfo Contains(MaterialInfo material)
	{
		for (MaterialInfo thisMaterial : this.materials) {
			if (material.typeID == thisMaterial.typeID && (thisMaterial.allDataValues || material.data == thisMaterial.data)) {
				return thisMaterial;
			} else if (thisMaterial.typeID > material.typeID) {
				return null;
			}
		}
		
		return null;
	}
	
	@Override
	public String toString()
	{
		StringBuilder stringBuilder = new StringBuilder();
		for (MaterialInfo material : this.materials) {
			stringBuilder.append(material.toString()).append(" ");
		}
		
		return stringBuilder.toString();
	}
	
	public int size()
	{
		return this.materials.size();
	}

	public void clear() 
	{
		this.materials.clear();
	}
}
