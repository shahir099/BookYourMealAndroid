package com.example.shahir.orderyourfood;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.example.shahir.orderyourfood.Common.Common;
import com.example.shahir.orderyourfood.Interface.ItemClickListener;
import com.example.shahir.orderyourfood.Model.Category;
import com.example.shahir.orderyourfood.ViewHolder.MenuViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.UUID;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class TestFoodDetail extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    TextView  txtFullName;

    // Firebase
    FirebaseDatabase database;
    DatabaseReference categories;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseRecyclerAdapter<Category,MenuViewHolder> adapter;


    /// View
    RecyclerView recycler_menu;
    RecyclerView.LayoutManager layoutManager;

    /// New menu layout

    MaterialEditText edtName;
    Button btnUpload,btnSelect;
    Category newCategory;

    Uri saveUri;
    private  final  int PICK_IMAGE_REQUEST=71;

    View add_menu_layout;

    DrawerLayout drawer;

    String foodId="";

    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/restaurant_font.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);



        toolbar.setTitle("Menu Management");
        setSupportActionBar(toolbar);

        // Init Firebase
        database = FirebaseDatabase.getInstance();
        categories = database.getReference("Foods");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();


        if(getIntent() != null){
            foodId=getIntent().getStringExtra("MenuId");
        }


/*        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });*/

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        /// Set Full Name of the User

        View headerView = navigationView.getHeaderView(0);
        txtFullName=(TextView) headerView.findViewById(R.id.txtFullName);
        //txtFullName.setText(Common.currentUser.getName());

        ///Init View
        recycler_menu = (RecyclerView) findViewById(R.id.recycler_menu);
        recycler_menu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_menu.setLayoutManager(layoutManager);

        loadMenu();
    }

    private void showDialog() {
        AlertDialog.Builder alertDialog=new AlertDialog.Builder(TestFoodDetail.this);
        alertDialog.setTitle("Add new Category");
        alertDialog.setMessage("Please full fill all informations");


        LayoutInflater inflater=this.getLayoutInflater();
        add_menu_layout = inflater.inflate(R.layout.add_new_menu_layout,null);

        edtName = add_menu_layout.findViewById(R.id.edtName);

        btnSelect=add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload=add_menu_layout.findViewById(R.id.btnUpload);


        /// Event for button

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage(); /// user will select image ffrom mobile
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();

            }
        });

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_add_shopping_cart_black_24dp);

        /// now set buttons

        alertDialog.setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                //// Create new Category

                if (newCategory != null)
                {
                    categories.push().setValue(newCategory);
                    Snackbar.make(drawer,"New Category "+newCategory.getName()+" is added",Snackbar.LENGTH_SHORT).show();
                }

            }
        });
        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });
        alertDialog.show();

    }

    private void uploadImage() {
        if(saveUri != null)
        {
            final ProgressDialog mDialog=new ProgressDialog(this);
            mDialog.setMessage("Uploading...Wait");
            mDialog.show();

            String imageName= UUID.randomUUID().toString();
            final StorageReference imageFolder= storageReference.child("images/"+imageName);
            imageFolder.putFile(saveUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mDialog.dismiss();
                            Toast.makeText(TestFoodDetail.this,"Uploaded !!!",Toast.LENGTH_SHORT).show();
                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    newCategory = new Category(edtName.getText().toString(),uri.toString(),foodId);

                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    mDialog.dismiss();
                    Toast.makeText(TestFoodDetail.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    /// no error just warnings

                    double progress =(int) (100.0* taskSnapshot.getBytesTransferred() /taskSnapshot.getTotalByteCount());
                    mDialog.setMessage("Uploaded "+ progress+"%");
                }
            });

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() !=null)
        {
            saveUri = data.getData();
            btnSelect.setText("Image Selected");
            Snackbar.make(drawer,"SELECTED ",Snackbar.LENGTH_SHORT).show();

        }
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST);
    }

    private void loadMenu() {
        adapter = new FirebaseRecyclerAdapter<Category, MenuViewHolder>(
                Category.class,
                R.layout.menu_item,
                MenuViewHolder.class,
                categories.orderByChild("MenuId").equalTo(foodId)
        ) {
            @Override
            protected void populateViewHolder(MenuViewHolder viewHolder, Category model, int position) {
                viewHolder.txtMenuName.setText(model.getName());
                Picasso.with(TestFoodDetail.this).load(model.getImage())
                        .into(viewHolder.imageView);


                viewHolder.setItemClickListener(new ItemClickListener(){

                    public void  onClick(View view,int position,boolean isLongClick){



                    }
                });


            }
        };

        adapter.notifyDataSetChanged(); // Refresh data if data changed
        recycler_menu.setAdapter(adapter);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_orders) {
            Intent orders=new Intent(TestFoodDetail.this,OrderStatus.class);
            startActivity(orders);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // update // delete writing here

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        if(item.getTitle().equals(Common.UPDATE)){
            showUpdateDialog(adapter.getRef(item.getOrder()).getKey(),adapter.getItem(item.getOrder()));
        }

        else if(item.getTitle().equals(Common.DELETE)){
            deleteCategory(adapter.getRef(item.getOrder()).getKey());
        }

        return super.onContextItemSelected(item);
    }

    private void deleteCategory(String key) {
        categories.child(key).removeValue();
        Snackbar.make(drawer,"Deleted ",Snackbar.LENGTH_SHORT).show();
    }

    private void showUpdateDialog(final String key, final Category item) {

        /// same code of showDialog with modification

        AlertDialog.Builder alertDialog=new AlertDialog.Builder(TestFoodDetail.this);
        alertDialog.setTitle("Update Category");
        alertDialog.setMessage("Please full fill all informations");


        LayoutInflater inflater=this.getLayoutInflater();
        add_menu_layout = inflater.inflate(R.layout.add_new_menu_layout,null);

        edtName = add_menu_layout.findViewById(R.id.edtName);

        btnSelect=add_menu_layout.findViewById(R.id.btnSelect);
        btnUpload=add_menu_layout.findViewById(R.id.btnUpload);

        ///Default name

        edtName.setText(item.getName());


        /// Event for button

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage(); /// user will select image ffrom mobile
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeImage(item);

            }
        });

        alertDialog.setView(add_menu_layout);
        alertDialog.setIcon(R.drawable.ic_add_shopping_cart_black_24dp);

        /// now set buttons

        alertDialog.setPositiveButton("CONFIRM", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                ///updating info
                item.setName(edtName.getText().toString());
                categories.child(key).setValue(item);

            }
        });
        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });
        alertDialog.show();

    }

    private void changeImage(final Category item) {
        if(saveUri != null)
        {
            final ProgressDialog mDialog=new ProgressDialog(this);
            mDialog.setMessage("Uploading...Wait");
            mDialog.show();

            String imageName= UUID.randomUUID().toString();
            final StorageReference imageFolder= storageReference.child("images/"+imageName);
            imageFolder.putFile(saveUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            mDialog.dismiss();
                            Toast.makeText(TestFoodDetail.this,"Uploaded !!!",Toast.LENGTH_SHORT).show();
                            imageFolder.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    item.setImage(uri.toString());
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    mDialog.dismiss();
                    Toast.makeText(TestFoodDetail.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    /// no error just warnings

                    double progress =(int) (100.0* taskSnapshot.getBytesTransferred() /taskSnapshot.getTotalByteCount());
                    mDialog.setMessage("Uploaded "+ progress+"%");
                }
            });

        }
    }
}
