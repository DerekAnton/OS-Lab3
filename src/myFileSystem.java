import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;




class myFileSystem
{

private File diskFile;
private RandomAccessFile disk;
private static LinkedList<String> inputCommands = new LinkedList<String>();

public static void main(String[] args) throws FileNotFoundException{
	
	
	
	//Get Commands From input file
	BufferedReader reader = new BufferedReader(new FileReader("inputSimple.txt"));
	String line = null;
	try {
		while ((line = reader.readLine()) != null) {
		    inputCommands.add(line);
		}
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

	String diskName = inputCommands.remove();
	CreateFS create = new CreateFS();
	create.createFS(diskName);
	myFileSystem fileSystem = new myFileSystem(diskName);
	
	
	String[] commands;
	
	
	//Run Commands
	for(int x = 0 ; x < inputCommands.size() - 1 ; x ++){
		commands = inputCommands.remove().split(" ");

		if(commands[0].equals("C")){
			fileSystem.create(commands[1].toCharArray(), Integer.parseInt(commands[2]));
		}
		if(commands[0].equals("d")){
			fileSystem.detete(commands[1].toCharArray());
		}
		if(commands[0].equals("r")){
			fileSystem.read(commands[1].toCharArray(), Integer.parseInt(commands[2]), commands[3].toCharArray());
		}
		if(commands[0].equals("w")){	
			fileSystem.write(commands[1].toCharArray(), Integer.parseInt(commands[2]), commands[3].toCharArray());
		}	
		if(commands[0].equals("l")){	
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
{ //create a file with this name and this size

  // high level pseudo code for creating a new file

  // Step 1: check to see if we have sufficient free space on disk by
  // reading in the free block list. To do this:
  // move the file pointer to the start of the disk file.
  // Read the first 128 bytes (the free/in-use block information)
  // Scan the list to make sure you have sufficient free blocks to
  // allocate a new file of this size
  int[] freeBlocks = new int[128];
  boolean space = false;
  int[] blockPointer = new int[8];
  
  for(int x = 0; x < 128; x++){
	  try {
		disk.seek(x);
		freeBlocks[x] = disk.read();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }
  
  int freeBlockNum = 0;
  for(int i : freeBlocks){
	  if(i == 0){
		  freeBlockNum++;
	  }
  }
  
  if(size < freeBlockNum){
	  space = true;
  }
  
  
  // Step 2: we look  for a free inode om disk
  // Read in a inode
  // check the "used" field to see if it is free
  // If not, repeat the above two steps until you find a free inode
  // Set the "used" field to 1
  // Copy the filename to the "name" field
  // Copy the file size (in units of blocks) to the "size" field

 // 128 + 2*8 + 4 +  8*4 + 4
  //128+52 180
  int used = 0;
  int freeINode = -1;
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
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 	  
  }
  // Step 3: Allocate data blocks to the file
  // for(i=0;i<size;i++)
    // Scan the block list that you read in Step 1 for a free block
    // Once you find a free block, mark it as in-use (Set it to 1)
    // Set the blockPointer[i] field in the inode to this block number.
    // 
 // end for
  if(space){
	  
	  for(int i = 0 ; i < size; i++){
		  int counter = 0;
		  for(int x : freeBlocks){
			  if(x == 0){
				  freeBlocks[counter] = 1;
				  blockPointer[size] = counter;
			  }
			  counter++;
		  }  
	  }
	  
  
  // Step 4: Write out the inode and free block list to disk
  //  Move the file pointer to the start of the file 
  // Write out the 128 byte free block list
  // Move the file pointer to the position on disk where this inode was stored
  // Write out the inode
	  try {
		//Write Free Block List
		for(int x = 0; x < 128; x++){
			disk.seek(x);
			disk.write(freeBlocks[x]);
		}
		
		//Write Name
		disk.seek(freeINode);
		for(int x = 0; x < 8; x++){
			disk.writeChar(name[x]);
			disk.seek(freeINode + 2*x); // multiply by two because we are writing to a new char, which is 2 bytes
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



public int detete(char name[])
{
  // Delete the file with this name

  // Step 1: Locate the inode for this file
  // Move the file pointer to the 1st inode (129th byte)
  // Read in a inode
  // If the iinode is free, repeat above step.
  // If the iinode is in use, check if the "name" field in the
  // inode matches the file we want to delete. IF not, read the next
  //  inode and repeat
  
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
	return 0;

} // End ls

//buff 1024
public int read(char name[], int blockNum, char buf[]){

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


public int write(char name[], int blockNum, char buf[])
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

	return 0;

} // end write

}