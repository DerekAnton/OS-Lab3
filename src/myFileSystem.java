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
	CreateFS create = new CreateFS();
	create.createFS(diskName);
	
	//Instantiate new fileSystem Object
	myFileSystem fileSystem = new myFileSystem(diskName);
	
	//Current Command
	String command;
	String[] commands;
	
	//Number of commands
	int inputSize = inputCommands.size();
	
	//Run Commands
	for(int x = 0 ; x < inputSize; x ++){
		commands = new String[4];
		command = inputCommands.remove().replaceAll("\\s+", " ");
		commands = command.split(" ");
		
		//Call Create
		if(commands[0].equals("C")){	
			fileSystem.create(commands[1].toCharArray(), Integer.parseInt(commands[2]));
		}
		
		//Call Delete
		if(commands[0].equals("D")){
			fileSystem.delete(commands[1].toCharArray());
		}
		
		//Call Read
		if(commands[0].equals("R")){
			fileSystem.read(commands[1].toCharArray(), Integer.parseInt(commands[2]));
		}
		
		//Call Write
		if(commands[0].equals("W")){	
			fileSystem.write(commands[1].toCharArray(), Integer.parseInt(commands[2]));
		}	
		
		//Call LS
		if(commands[0].equals("L")){	
			fileSystem.ls();
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
  int[] blockPointer = new int[8];//Block Pointer storage
  
  //Read in Free Blocks
  for(int x = 0; x < 128; x++){
	  try {
		disk.seek(x);
		freeBlocks[x] = disk.read();
	} catch (IOException e) {
		e.printStackTrace();
	}
  }

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
			space = true;
			break;
		}
	} catch (IOException e) {
		e.printStackTrace();
	} 	  
  }
 
  //No free iNode
  if(space == false){
	  System.out.println("not a free inode on disk");
  }
  
  
  //If there is room for the file, create it and store it on desk
  if(space){
	  for(int i = 0 ; i < size; i++){
		  for(int counter = 0; counter < freeBlocks.length ; counter++){
			  if(freeBlocks[counter] == 0){
				  freeBlocks[counter] = 1;
				  blockPointer[i] = counter;
				  break;
			  }
		  }  
	  }
	  
  

	  try {
		//Write Free Block List
		for(int x = 0; x < 128; x++){
			disk.seek(x);
			disk.write(freeBlocks[x]);
		}
		
		//Write Name
		disk.seek(freeINode);
		for(int x = 0; x < name.length; x++){
			disk.writeChar(name[x]);

			disk.seek(freeINode + 2*(x+1)); // multiply by two because we are writing to a new char, which is 2 bytes
		}
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
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  
  }
  
  
  return  (space) ? 1 : 0;
} // End Create



public int delete(char[] name) 
{
  // Delete the file with this name

  // Step 1: Locate the inode for this file
  // Move the file pointer to the 1st inode (129th byte)
  // Read in a inode
  // If the iinode is free, repeat above step.
  // If the iinode is in use, check if the "name" field in the
  // inode matches the file we want to delete. IF not, read the next
  //  inode and repeat
		try {
			int usedBit = 0;
			int iNodeStart = 0;
			char[] currentName = new char[name.length];
			int[] freeBlocksToDelete = new int[8];
			int iNodeToDelete;
			for (int i = 0; i < 16; i++) {
				usedBit = (128 + 16 + 4 + 32 + 56 * i);
				iNodeStart = usedBit - (16 + 4 + 32);
				disk.seek(usedBit);
				int isUsed = disk.readInt();
				if (isUsed == 1) {
					
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
						for (int x = 0; x < 8; x++) {
							disk.seek(iNodeStart + 20 + 4 * x);
							freeBlocksToDelete[x] = disk.readInt();
						}
						
						
						//Update Free Blocks
						for(int pointer : freeBlocksToDelete){
							disk.seek(pointer);
							disk.write(0);
						}

					}
				}

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
  // Step 2: free blocks of the file being deleted
  // Read in the 128 byte free block list (move file pointer to start
 // of the disk and read in 128 bytes)
  // Free each block listed in the blockPointer fields as follows:
  // for(i=0;i< inode.size; i++) 
     // freeblockList[ inode.blockPointer[i] ] = 0;

  // Step 3: mark inode as free
  // Set the "used" field to 0.

  // Step 4: Write out the inode and free block list to disk
  //  Move the file pointer to the start of the file 
  // Write out the 128 byte free block list
  // Move the file pointer to the position on disk where this inode was stored
  // Write out the inode
	return 0;

} // End Delete


public int ls()
{ 
  // List names of all files on disk

  // Step 1: read in each inode and print!
  // Move file pointer to the position of the 1st inode (129th byte)
  // for(i=0;i<16;i++)
    // REad in a inode
    // If the inode is in-use
      // print the "name" and "size" fields from the inode
 // end for
	
	try {
		int usedBit = 0;
		int iNodeStart = 0;
		for(int i = 0 ; i < 16 ; i++){
			usedBit = (128 + 16 + 4 + 32 + 56*i);
			iNodeStart = usedBit - (16+4+32);
			disk.seek(usedBit);
			int isUsed = disk.readInt();
			//System.out.println(isUsed + " " + usedBit);
			if(isUsed == 1){
				//Read Name
				System.out.print("File Name: ");
				for(int x = 0; x < 8; x++){
					disk.seek(iNodeStart +2*x);
					System.out.print(disk.readChar());
				}
				System.out.print(" ");

				//Read Size
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
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return 0;

} // End ls

//buff 1024
public int read(char[] name, int blockNum){ // char buf[] needs to come out 

	
	
	try {
		int usedBit = 0;
		int iNodeStart = 0;
		char[] currentName = new char[name.length];
		int[] freeBlocksToRead = new int[8];
		int iNodeToRead;
		for (int i = 0; i < 16; i++) {
			usedBit = (128 + 16 + 4 + 32 + 56 * i);
			iNodeStart = usedBit - (16 + 4 + 32);
			disk.seek(usedBit);
			int isUsed = disk.readInt();
			if (isUsed == 1) {
				
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
					for (int x = 0; x < 1024; x++) {
						disk.seek(freeBlocksToRead[blockNum] * 1024 + x);
						buffer[x] = (byte)disk.read(); // MAY BE PROBLEMATIC
					}

				}
			}

		}
	}catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	
	
	
	
   // read this block from this file
   
   // Step 1: locate the inode for this file
   // Move file pointer to the position of the 1st inode (129th byte)
   // Read in a inode
   // If the inode is in use, compare the "name" field with the above file
   // IF the file names don't match, repeat

   // Step 2: Read in the specified block
   // Check that blockNum < inode.size, else flag an error
   // Get the disk address of the specified block
   // That is, addr = inode.blockPointer[blockNum]
   // move the file pointer to the block location (i.e., to byte #
   //addr*1024 in the file)

    // Read in the block! => Read in 1024 bytes from this location
   // into the buffer "buf"
	return 0;
 
} // End read


public int write(char[] name, int blockNum)// char buf[] needs to come out 
{

   // write this block to this file

   // Step 1: locate the inode for this file
   // Move file pointer to the position of the 1st inode (129th byte)
   // Read in a inode
   // If the inode is in use, compare the "name" field with the above file
   // IF the file names don't match, repeat

   // Step 2: Write to the specified block
   // Check that blockNum < inode.size, else flag an error
   // Get the disk address of the specified block
   // That is, addr = inode.blockPointer[blockNum]
   // move the file pointer to the block location (i.e., byte # addr*1024)

    // Write the block! => Write 1024 bytes from the buffer "buff" to 
       // this location
	

		try {
			int usedBit = 0;
			int iNodeStart = 0;
			char[] currentName = new char[name.length];
			int[] blockPointers = new int[8];
			int iNodeToRead;
			for (int i = 0; i < 16; i++) {
				usedBit = (128 + 16 + 4 + 32 + 56 * i);
				iNodeStart = usedBit - (16 + 4 + 32);
				disk.seek(usedBit);
				int isUsed = disk.readInt();
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
							
							for (int x = 0; x < 8; x++) {
								disk.seek(iNodeStart + 20 + 4 * x);
								blockPointers[x] = disk.readInt();
							}
							
							for (int x = 0; x < 1024; x++) {
								disk.seek(blockPointers[blockNum] * 1024 + x);
								disk.write(buffer[x]);
							}
						}
					}
				}

			}
	}catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	

	return 0;

} // end write

}