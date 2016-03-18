/** Statistical Natural Language Processing System
    Copyright (C) 2014-2015  Lu, Wei

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
/**
 * 
 */
package tmp.experiments;

/**
 * @author wei_lu
 *
 */
public class Speedcheck {
	
	public static void main(String args[]){
		
		long time;
		time = System.currentTimeMillis();
		for(int i = 0; i<10000; i++){
			for(int j = 0; j<10000; j++){
				boolean v = true;
			}
		}
		time = System.currentTimeMillis() - time;
		System.err.println("Time:"+time/1000.0+" seconds.");
		
	}

}
