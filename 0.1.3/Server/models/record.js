// models/record.js
const Sequelize = require('sequelize');

module.exports = class Record extends Sequelize.Model {
    static init(sequelize) {
        return super.init({
            record_id: {  
                type: Sequelize.INTEGER,
                autoIncrement: true,
                primaryKey: true,
            },
            email: {
                type: Sequelize.STRING(100),
                allowNull: false,
            },
            date: {
                type: Sequelize.DATE,
                allowNull: false,
            },
            is_sleep: {     
                type: Sequelize.BOOLEAN,
                allowNull: false,
                defaultValue: false,
            },
        }, {
            sequelize,
            timestamps: true,
            underscored: false,
            modelName: 'Record',
            tableName: 'records',
            paranoid: true,
            charset: 'utf8mb4',
            collate: 'utf8mb4_general_ci',
        });
    }

    static associate(db) {
        db.Record.belongsTo(db.User, { foreignKey: 'email', targetKey: 'email', onDelete: 'CASCADE' });
    }
};
