import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;


class myFileSystem
{

private File diskFile; //Disk File
private RandomAccessFile disk; //Random Access File used to do operations
private static LinkedList<String> inputCommands = new LinkedList<String>(); //Commands from input file
private byte [] buffer = new byte [1024]; // Dummy Buffer

public static void main(String[] args) throws FileNotFoundException{
	
	BufferedReader reader = new BufferedReader(new FileReader("input.txt"));
	String line = null;
	try {
		//Grab all the commands from the input file
		while ((line = reader.readLine()) != null) {
		    inputCommands.add(line);
		}
	} catch (IOException e) {
		e.printStackTrace();
	}

	//First line from file contains disk name
	String diskName = inputCommands.remove();
	diskName = diskName.replaceAll("\\s+", " ");
	
	//Create new  disk file 
	//CreateFS create = new CreateFS();
	//create.createFS(diskName);
	
	//Instantiate new fileSystem Object
	myFileSystem fileSystem = new myFileSystem(diskName);
	
	//Current Command
	String command;
	String[] commands;
	System.out.println( );

	
	//Number of commands
	int inputSize = inputCommands.size();
	
	//Run Commands
	for(int x = 0 ; x < inputSize; x ++){
		commands = new String[4];
		command = inputCommands.remove().replaceAll("\\s+", " ");
		commands = command.split(" ");
		
		//Call Create
		if(commands[0].equals("C")){	
			System.out.println("Creating File: " + commands[1] + " with size: " +Integer.parseInt(commands[2]) );
			fileSystem.create(commands[1].toCharArray(), Integer.parseInt(commands[2]));
			System.out.println( );
			System.out.println( );
		}
		
		//Call Delete
		if(commands[0].equals("D")){
			System.out.println("Deleting File: " + commands[1]);
			fileSystem.delete(commands[1].toCharArray());
			System.out.println( );System.out.println( );
		}
		
		//Call Read
		if(commands[0].equals("R")){
			System.out.println("Reading File: " + commands[1] + " From Block:  " + Integer.parseInt(commands[2]));
			fileSystem.read(commands[1].toCharArray(), Integer.parseInt(commands[2]));
			System.out.println( );System.out.println( );
		}
		
		//Call Write
		if(commands[0].equals("W")){	
			System.out.println("Writing File: " + commands[1] + " From Block: " + Integer.parseInt(commands[2]));
			fileSystem.write(commands[1].toCharArray(), Integer.parseInt(commands[2]));
			System.out.println( );System.out.println( );
		}	
		
		//Call LS
		if(commands[0].equals("L")){	
			System.out.println("------------LS START------------");
			fileSystem.ls();
			System.out.println("------------LS END--------------");
			System.out.println( );System.out.println( );
		}

	}
	
	
}
public myFileSystem(String diskName) throws FileNotFoundException{
   // open the file with the above name
   // this file will act as the "disk" for your file system
	diskFile = new File(diskName);
	disk = new RandomAccessFile(diskFile, "rw");
	
}



public int create(char[] name, int size)
{ 
  
  int[] freeBlocks = new int[128]; //Free Block List Storage
  boolean space = false;  //Is there spaceto create file
  boolean inodeFree = false; //Is there inode available
  int[] blockPointer = new int[8];//Block Pointer storage
  
  //Read in Free Blocks
  System.out.print("Free Block List: ");
  for(int x = 0; x < 128; x++){
	  try {
		disk.seek(x);
		freeBlocks[x] = disk.read();
		System.out.print(freeBlocks[x]);
	} catch (IOException e) {
		e.printStackTrace();
	}
  }
  System.out.println("");
  
  
  //Count number of free blocks
  int freeBlockNum = 0;
  for(int i : freeBlocks){
	  if(i == 0){
		  freeBlockNum++;
	  }
  }
  
  //Is there enough room?
  if(size < freeBlockNum){
	  space = true;
  }else{
	  //Not enough room
	  System.out.println("not enough space on disk");
  }
  
 
  
  int used = 0;
  int freeINode = -1;
  //Read in all the iNodes, looking at the used int.
  //If a free block is found, stores is
  for(int x = 0 ; x < 16 ; x++){
	  try {
		disk.seek(180 + (56*x));
		used = disk.readInt();
		if(used == 0){
			freeINode = (128 + (56*x));
			  System.out.println(freeINode);
			  inodeFree = true;
			break;
		}
	} catch (IOException e) {
		e.printStackTrace();
	} 	  
  }
  
  //No free iNode
  if(inodeFree == false){
	  System.out.println("not a free inode on disk");
  }
  
  
  //If there is room for the file, create it and store it on desk
  if(space && inodeFree){
	  System.out.print("Block Storage Locations: ");
	  for(int i = 0 ; i < size; i++){
		  for(int counter = 0; counter < freeBlocks.length ; counter++){
			  if(freeBlocks[counter] == 0){
				  System.out.print(" " + counter + " ");
				  freeBlocks[counter] = 1;
				  blockPointer[i] = counter;
				  break;
			  }
		  }  
	  }
	  System.out.println();
  

	  try {
		//Write Free Block List
		  System.out.print("New Free Block List: ");
		for(int x = 0; x < 128; x++){
			disk.seek(x);
			disk.write(freeBlocks[x]);
			System.out.print(freeBlocks[x]);
		}
		  System.out.println();

		
		//Write Name
		  System.out.println(freeINode);
		disk.seek(freeINode);
		for(int x = 0; x < name.length; x++){
			disk.writeChar(name[x]);
			disk.seek(freeINode + 2*(x+1)); // multiply by two because we are writing to a new char, which is 2 bytes
		}
		
		//Fill rest of name array with spaces
		for(int x = (8-(8-name.length)); x < name.length ; x++){
			disk.seek(freeINode + 2*(8-x));
			disk.writeChar('\0');
		}
		
		//Write Size
		disk.seek(freeINode + 16); // 16 because we ended on 8*2 in the above loop, so we must seek after where this value has been written to.
		disk.writeInt(size);

		
		//Write BlackPointers
		for(int x = 0 ; x < 8 ; x++){
			disk.seek(freeINode + 20 + 4*x); // start at 20 because the above writes the size to the next integer (4 bytes)  and multiply by two because we are writing to a new int, which is 4 bytes
			disk.writeInt(blockPointer[x]);			
		}
		
		//Write Used
		disk.seek(freeINode + 52); // 52 is just the last position before the int we are going to write too
		disk.writeInt(1);
		
	} catch (IOException e) {
		e.printStackTrace();
	}
  
  }
  
  
  return  (space) ? 1 : 0;
} // End Create



public int delete(char[] name) 
{
		try {
			int usedBit = 0;
			int iNodeStart = 0;
			char[] currentName = new char[name.length];
			int[] freeBlocksToDelete = new int[8];
			int iNodeToDelete;
			
			//Loop through all the iNodes
			for (int i = 0; i < 16; i++) {
				usedBit = (128 + 16 + 4 + 32 + 56 * i);//address for used bit
				iNodeStart = usedBit - (16 + 4 + 32); //Address for inode start
				disk.seek(usedBit);
				int isUsed = disk.readInt();
				
				
				if (isUsed == 1) {
					//Check to see if it is file we are looking for
					for (int x = 0; x < name.length; x++) {
						disk.seek(iNodeStart + 2 * x);
						currentName[x] = disk.readChar();
					}

					//Is this the file you wish to delete?
					if (String.valueOf(name).toString().equals(String.valueOf(currentName))) {
						disk.seek(usedBit);
						disk.writeInt(0);
						
						//Look for what which blocks to free
						iNodeToDelete = iNodeStart;
						System.out.print( "Blocks To Deleted");
						for (int x = 0; x < 8; x++) {
							disk.seek(iNodeStart + 20 + 4 * x);
							freeBlocksToDelete[x] = disk.readInt();
							System.out.print( " " + freeBlocksToDelete[x] + " ");
						}
						System.out.println( );
						
						//Update Free Blocks
						for (int pointer : freeBlocksToDelete) {
							if (pointer != 0) { //If not super
								disk.seek(pointer);
								disk.write(0);
							}
						}

					}
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	return 0;

} // End Delete


public int ls()
{ 
	try {
		int usedBit = 0;
		int iNodeStart = 0;
		
		//Loop through inodes
		for(int i = 0 ; i < 16 ; i++){
			usedBit = (128 + 16 + 4 + 32 + 56*i);//used bit address 
			iNodeStart = usedBit - (16+4+32); //inode address
			disk.seek(usedBit);
			int isUsed = disk.readInt(); 

			//Is inode in use
			if(isUsed == 1){
				
				//Read and print Name
				System.out.print("File Name: ");
				for(int x = 0; x < 8; x++){
					disk.seek(iNodeStart +2*x);
					System.out.print(disk.readChar());
				}
				System.out.print(" ");

				//Read and print Size
				System.out.print("  File Size: ");
				disk.seek(iNodeStart + 16);
				System.out.print(disk.readInt());

				System.out.print(" ");

				//Read Block Pointers
				/*
				System.out.print("Block Pointers: ");
				for(int x = 0 ; x < 8 ; x++){
					disk.seek(iNodeStart + 20 + 4*x); 
					System.out.print(disk.readInt());
				}
				
				System.out.print(" ");
				System.out.print("File Name: ");
				System.out.print(1);
				*/
				System.out.println("");

			}
		}
		
	} catch (IOException e) {
		e.printStackTrace();
	}
	return 0;

} // End ls


public int read(char[] name, int blockNum){ 
	try {
		int usedBit = 0;
		int iNodeStart = 0;
		char[] currentName = new char[name.length];
		int[] freeBlocksToRead = new int[8];
		int iNodeToRead;
		
		//Read inodes
		for (int i = 0; i < 16; i++) {
			usedBit = (128 + 16 + 4 + 32 + 56 * i);//address of used bit
			iNodeStart = usedBit - (16 + 4 + 32);//inode start bit
			disk.seek(usedBit);
			int isUsed = disk.readInt();
			
			//Is block in use
			if (isUsed == 1) {
				//read name
				for (int x = 0; x < name.length; x++) {
					disk.seek(iNodeStart + 2 * x);
					currentName[x] = disk.readChar();
				}

				//Is this the file you wish to read?
				if (String.valueOf(name).toString().equals(String.valueOf(currentName))) {
					disk.seek(iNodeStart + 16);
					int size = disk.readInt();
					
					
					//Look for what which blocks to free
					iNodeToRead = iNodeStart;
					for (int x = 0; x < 8; x++) {
						disk.seek(iNodeStart + 20 + 4 * x);
						freeBlocksToRead[x] = disk.readInt();
					}
					
					disk.seek(freeBlocksToRead[blockNum] * 1024);
					
					//read data into buffers
					System.out.print("Data from block: " );
					for (int x = 0; x < 1024; x++) {
						disk.seek(freeBlocksToRead[blockNum] * 1024 + x);
						buffer[x] = (byte)disk.read(); 
						System.out.print( buffer[x]);
					}
					System.out.println( );

				}
			}

		}
	}catch (IOException e) {
		e.printStackTrace();
	}
	
	
	
	

	return 0;
 
} // End read


public int write(char[] name, int blockNum)// char buf[] needs to come out 
{

		try {
			int usedBit = 0;
			int iNodeStart = 0;
			char[] currentName = new char[name.length];
			int[] blockPointers = new int[8];
			int iNodeToRead;

			//loop through inodes
			for (int i = 0; i < 16; i++) {
				usedBit = (128 + 16 + 4 + 32 + 56 * i);//used address
				iNodeStart = usedBit - (16 + 4 + 32);//inode address
				disk.seek(usedBit);
				int isUsed = disk.readInt();
				
				//is inode used 
				if (isUsed == 1) {

					for (int x = 0; x < name.length; x++) {
						disk.seek(iNodeStart + 2 * x);
						currentName[x] = disk.readChar();
					}
					disk.seek(iNodeStart + 16);
					int size = disk.readInt();
					
					// Is this the file you wish to read?
					if (String.valueOf(name).toString()
							.equals(String.valueOf(currentName))) {
						if (blockNum < size) {
							// Look for what which blocks to free
							iNodeToRead = iNodeStart;
							
							//read in block pointers
							for (int x = 0; x < 8; x++) {
								disk.seek(iNodeStart + 20 + 4 * x);
								blockPointers[x] = disk.readInt();
							}
							//write data from buffers
							System.out.println("Buffer Data: " );
							for (int x = 0; x < 1024; x++) {
								disk.seek(blockPointers[blockNum] * 1024 + x);
								disk.write(buffer[x]);
								System.out.print(buffer[x] );
							}
							System.out.println( );
						}
					}
				}

			}
	}catch (IOException e) {
		e.printStackTrace();
	}
	
	

	return 0;

} // end write

}