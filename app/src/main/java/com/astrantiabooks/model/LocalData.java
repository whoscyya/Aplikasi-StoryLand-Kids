package com.astrantiabooks.models;

import com.astrantiabooks.R;
import java.util.ArrayList;
import java.util.List;

public class LocalData {
    public static List<Buku> listBuku = new ArrayList<>();
    public static User currentUser; // Menyimpan siapa yang sedang login

    public static void deleteBuku(String id) {
        for (int i = 0; i < listBuku.size(); i++) {
            if (listBuku.get(i).getId().equals(id)) {
                listBuku.remove(i);
                break;
            }
        }
    }
}