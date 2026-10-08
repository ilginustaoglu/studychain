require('dotenv').config(); // .env dosyasındaki şifreleri okumak için
const express = require('express');
const { Pool } = require('pg');

const app = express();
// Render bir port verirse onu kullan, yoksa 3000'i kullan
const PORT = process.env.PORT || 3000;

app.use(express.json()); // Gelen JSON verilerini okuyabilmek için

// Veritabanı bağlantı havuzu oluşturuluyor
const pool = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: {
    rejectUnauthorized: false // Render'ın güvenli bağlantısı (SSL) için zorunludur
  }
});

// Sitenizin ana sayfasına girildiğinde çalışacak test rotası
app.get('/', (req, res) => {
  res.send('Studychain Backend API başarıyla çalışıyor!');
});

// Veritabanı bağlantısını test etmek için özel bir rota
app.get('/db-test', async (req, res) => {
  try {
    const result = await pool.query('SELECT NOW()');
    res.json({ 
      mesaj: "Veritabanına harika bir şekilde bağlandık!", 
      zaman: result.rows[0].now 
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({ hata: "Veritabanına bağlanılamadı." });
  }
});

// Sunucuyu başlat
app.listen(PORT, () => {
  console.log(`Sunucu ${PORT} numaralı portta ayağa kalktı...`);
});