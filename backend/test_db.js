const mysql = require('mysql2/promise');

async function testPasswords() {
  const passwords = ['', 'root', '1234', '123456', 'admin', 'password', 'mysql', 'root1234', 'Campus@123'];
  for (const pwd of passwords) {
    try {
      const conn = await mysql.createConnection({
        host: '127.0.0.1',
        port: 3306,
        user: 'root',
        password: pwd
      });
      console.log('SUCCESS! Password for root is:', JSON.stringify(pwd));
      await conn.end();
      return pwd;
    } catch (e) {
      console.log('Tested password ' + JSON.stringify(pwd) + ' -> ' + e.message);
    }
  }
}

testPasswords();
